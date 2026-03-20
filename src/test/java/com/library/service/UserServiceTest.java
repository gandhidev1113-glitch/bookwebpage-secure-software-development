package com.library.service;

import com.library.model.Role;
import com.library.model.User;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_encodesPassword_andSavesUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@lib.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.register("alice", "alice@lib.com", "StrongPass123", Role.USER);

        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getEmail()).isEqualTo("alice@lib.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        verify(passwordEncoder).encode("StrongPass123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("alice", "alice@lib.com", "StrongPass123", Role.USER)
        );

        assertThat(ex.getMessage()).isEqualTo("Username already taken");
    }

    @Test
    void loadUserByUsername_mapsRoleToGrantedAuthority() {
        User user = new User();
        user.setUsername("librarian");
        user.setPassword("encoded");
        user.setRole(Role.LIBRARIAN);

        when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("librarian");

        assertThat(details.getUsername()).isEqualTo("librarian");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_LIBRARIAN");
    }

    @Test
    void loadUserByUsername_throwsWhenMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("ghost"));
    }
}
