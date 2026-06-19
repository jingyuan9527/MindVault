package com.mindvault.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auth.config.UserContext;
import com.mindvault.auth.dto.ChangePasswordRequest;
import com.mindvault.auth.dto.CreateTokenRequest;
import com.mindvault.auth.dto.LoginRequest;
import com.mindvault.auth.entity.ApiToken;
import com.mindvault.auth.entity.User;
import com.mindvault.auth.service.ApiTokenService;
import com.mindvault.auth.service.UserService;
import com.mindvault.auth.config.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ApiTokenService apiTokenService;

    @MockBean
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private User createUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setDisplayName("管理员");
        u.setRole("ADMIN");
        return u;
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() throws Exception {
        User user = createUser();
        when(userService.authenticate("admin", "pass123")).thenReturn(user);
        when(sessionManager.createSession(user)).thenReturn("session-uuid");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("session-uuid"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_shouldReturn401WhenInvalid() throws Exception {
        when(userService.authenticate("admin", "wrong")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void me_shouldReturnCurrentUser() throws Exception {
        User user = createUser();
        when(userService.getById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.displayName").value("管理员"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void me_shouldReturn401WhenNotLoggedIn() throws Exception {
        UserContext.clear();

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void changePassword_shouldSucceedWhenOldPasswordCorrect() throws Exception {
        when(userService.changePassword(1L, "old", "new")).thenReturn(true);

        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("old", "new"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void changePassword_shouldReturn400WhenOldPasswordWrong() throws Exception {
        when(userService.changePassword(1L, "wrong", "new")).thenReturn(false);

        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("wrong", "new"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createToken_shouldReturnToken() throws Exception {
        ApiToken token = new ApiToken();
        token.setId(1L);
        token.setToken("uuid-token-value");
        token.setName("my-app");
        token.setExpiresAt(LocalDateTime.now().plusDays(30));
        when(apiTokenService.createToken(1L, "my-app", 30)).thenReturn(token);

        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTokenRequest("my-app", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("uuid-token-value"))
                .andExpect(jsonPath("$.data.name").value("my-app"));
    }

    @Test
    void createToken_shouldReturn401WhenNotLoggedIn() throws Exception {
        UserContext.clear();

        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTokenRequest("x", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listTokens_shouldReturnTokens() throws Exception {
        ApiToken t = new ApiToken();
        t.setId(1L);
        t.setName("my-app");
        t.setCreatedAt(LocalDateTime.now());
        when(apiTokenService.listByUser(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/auth/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("my-app"));
    }

    @Test
    void deleteToken_shouldSucceed() throws Exception {
        doNothing().when(apiTokenService).deleteToken(1L, 5L);

        mockMvc.perform(delete("/api/v1/auth/tokens/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void logout_shouldRemoveSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(sessionManager).remove("session-token");
    }
}
