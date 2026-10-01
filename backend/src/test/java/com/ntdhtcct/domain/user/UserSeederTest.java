package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_createsDefaultCustomerUserWhenMissing() {
        UUID roleId = UUID.randomUUID();
        Role customerRole = new Role("CUSTOMER");
        setRoleId(customerRole, roleId);

        when(userRepository.existsByEmailIgnoreCase("test@test.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("12345678")).thenReturn("argon2-seeded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSeeder userSeeder = new UserSeeder(userRepository, roleRepository, passwordEncoder);
        userSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("test@test.com");
        assertThat(savedUser.getPassword()).isEqualTo("argon2-seeded-hash");
        assertThat(savedUser.getFullName()).isEqualTo("Test User");
        assertThat(savedUser.getRoleId()).isEqualTo(roleId);
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
}
