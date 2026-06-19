package com.mindvault.auth.service;

import com.mindvault.auth.entity.User;
import com.mindvault.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserService userService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper);
    }

    private User createUser(Long id, String username, String password, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName(username);
        u.setRole("USER");
        u.setEnabled(enabled);
        return u;
    }

    @Test
    void authenticate_shouldReturnUserWhenCredentialsValid() {
        User user = createUser(1L, "testuser", "pass123", true);
        when(userMapper.findByUsername("testuser")).thenReturn(user);

        User result = userService.authenticate("testuser", "pass123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void authenticate_shouldReturnNullWhenPasswordWrong() {
        User user = createUser(1L, "testuser", "correct", true);
        when(userMapper.findByUsername("testuser")).thenReturn(user);

        User result = userService.authenticate("testuser", "wrong");

        assertNull(result);
    }

    @Test
    void authenticate_shouldReturnNullWhenUserNotFound() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        User result = userService.authenticate("nobody", "pass");

        assertNull(result);
    }

    @Test
    void authenticate_shouldReturnNullWhenUserDisabled() {
        User user = createUser(1L, "disabled", "pass", false);
        when(userMapper.findByUsername("disabled")).thenReturn(user);

        User result = userService.authenticate("disabled", "pass");

        assertNull(result);
    }

    @Test
    void createUser_shouldInsertNewUser() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        User result = userService.createUser("newuser", "mypass", "New User", "ADMIN");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("New User", result.getDisplayName());
        assertEquals("ADMIN", result.getRole());
        assertTrue(encoder.matches("mypass", result.getPasswordHash()));
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameExists() {
        when(userMapper.findByUsername("exists")).thenReturn(createUser(1L, "exists", "pass", true));

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("exists", "pass", null, null));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void changePassword_shouldUpdateWhenOldPasswordCorrect() {
        User user = createUser(1L, "testuser", "oldpass", true);
        when(userMapper.selectById(1L)).thenReturn(user);

        boolean result = userService.changePassword(1L, "oldpass", "newpass");

        assertTrue(result);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void changePassword_shouldReturnFalseWhenOldPasswordWrong() {
        User user = createUser(1L, "testuser", "oldpass", true);
        when(userMapper.selectById(1L)).thenReturn(user);

        boolean result = userService.changePassword(1L, "wrong", "newpass");

        assertFalse(result);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void changePassword_shouldReturnFalseWhenUserNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        boolean result = userService.changePassword(99L, "old", "new");

        assertFalse(result);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void getById_shouldReturnUser() {
        User user = createUser(1L, "testuser", "pass", true);
        when(userMapper.selectById(1L)).thenReturn(user);

        User result = userService.getById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        User result = userService.getById(99L);

        assertNull(result);
    }

    @Test
    void listAll_shouldReturnAllUsers() {
        List<User> users = List.of(
                createUser(1L, "user1", "pass", true),
                createUser(2L, "user2", "pass", true)
        );
        when(userMapper.selectList(null)).thenReturn(users);

        List<User> result = userService.listAll();

        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
    }

    @Test
    void listAll_shouldReturnEmptyWhenNoUsers() {
        when(userMapper.selectList(null)).thenReturn(List.of());

        List<User> result = userService.listAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void setEnabled_shouldUpdateWhenUserExists() {
        User user = createUser(1L, "testuser", "pass", true);
        when(userMapper.selectById(1L)).thenReturn(user);

        boolean result = userService.setEnabled(1L, false);

        assertTrue(result);
        assertFalse(user.getEnabled());
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void setEnabled_shouldReturnFalseWhenUserNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        boolean result = userService.setEnabled(99L, false);

        assertFalse(result);
        verify(userMapper, never()).updateById(any(User.class));
    }
}
