package com.mindvault.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auth.config.UserContext;
import com.mindvault.auth.entity.User;
import com.mindvault.auth.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAdminController.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private User createUser(Long id, String username, String role, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(username);
        u.setRole(role);
        u.setEnabled(enabled);
        return u;
    }

    @Test
    void listUsers_shouldReturnUserList() throws Exception {
        when(userService.listAll()).thenReturn(List.of(
                createUser(1L, "admin", "ADMIN", true),
                createUser(2L, "testuser", "USER", true)
        ));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data[1].username").value("testuser"))
                .andExpect(jsonPath("$.data[1].role").value("USER"));
    }

    @Test
    void listUsers_shouldReturn403WhenNotAdmin() throws Exception {
        UserContext.set(new UserContext.UserInfo(2L, "user", "USER"));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void listUsers_shouldReturn403WhenNotLoggedIn() throws Exception {
        UserContext.clear();

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void setEnabled_shouldUpdateWhenAdmin() throws Exception {
        when(userService.setEnabled(2L, false)).thenReturn(true);

        mockMvc.perform(put("/api/v1/users/2/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).setEnabled(2L, false);
    }

    @Test
    void setEnabled_shouldReturn403WhenNotAdmin() throws Exception {
        UserContext.set(new UserContext.UserInfo(2L, "user", "USER"));

        mockMvc.perform(put("/api/v1/users/2/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verify(userService, never()).setEnabled(anyLong(), anyBoolean());
    }
}
