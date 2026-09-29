package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.role.Role;
import com.ntdhtcct.domain.role.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsCustomerRole() {
        UUID roleId = UUID.randomUUID();
        Role customerRole = new Role("CUSTOMER");
        setRoleId(customerRole, roleId);
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("argon2-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.register("  Example Person  ", " Person@Example.com ", "password123");

        assertThat(registered.getEmail()).isEqualTo("person@example.com");
        assertThat(registered.getFullName()).isEqualTo("Example Person");
        assertThat(registered.getPassword()).isEqualTo("argon2-hash");
        assertThat(registered.getPassword()).isNotEqualTo("password123");
        assertThat(registered.getRoleId()).isEqualTo(roleId);
        assertThat(registered.getFailedLoginAttempts()).isZero();
        assertThat(registered.getLockedUntil()).isNull();
    }

    private void setRoleId(Role role, UUID roleId) {
        try {
            Field idField = Role.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(role, roleId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not initialize role ID for test", e);
        }
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("Example Person", "person@example.com", "password123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email này đã được đăng ký.");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }
}