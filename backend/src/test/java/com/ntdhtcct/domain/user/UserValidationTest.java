package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-03.2: Unit test validation cho trường email và password của User entity.
 *
 * <p>Test không cần Spring context hay database — chỉ dùng Jakarta Validation API
 * thông qua Hibernate Validator trực tiếp.</p>
 */
class UserValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Tạo User với email và password được chỉ định thông qua reflection.
     * Sử dụng reflection để đặt giá trị vì constructor protected và setter
     * email/password không set giá trị null (String) trực tiếp trong constructor.
     */
    private User buildUser(String email, String password) {
        try {
            // Dùng constructor mặc định (protected — trong cùng package test)
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = (User) constructor.newInstance();

            setField(user, "email", email);
            setField(user, "password", password);
            setField(user, "createdAt", OffsetDateTime.now());
            setField(user, "updatedAt", OffsetDateTime.now());

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo User cho test: " + e.getMessage(), e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Set<ConstraintViolation<User>> validate(String email, String password) {
        User user = buildUser(email, password);
        return validator.validate(user);
    }

    private boolean hasViolationOnField(Set<ConstraintViolation<User>> violations, String fieldName) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName));
    }

    // -------------------------------------------------------------------------
    // Email — invalid cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("email null → vi phạm validation")
    void email_null_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate(null, "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email null phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("email rỗng → vi phạm validation")
    void email_blank_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("", "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email rỗng phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("email chỉ có khoảng trắng → vi phạm validation")
    void email_whitespaceOnly_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("   ", "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email chỉ có whitespace phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("email sai định dạng (thiếu @) → vi phạm validation")
    void email_missingAtSign_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("notanemail", "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email không có @ phải vi phạm @Email")
                .isTrue();
    }

    @Test
    @DisplayName("email sai định dạng (thiếu domain) → vi phạm validation")
    void email_missingDomain_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("user@", "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email thiếu domain phải vi phạm @Email")
                .isTrue();
    }

    @Test
    @DisplayName("email vượt quá 255 ký tự → vi phạm validation")
    void email_tooLong_shouldBeInvalid() {
        // Tạo email dài hơn 255 ký tự
        String longLocal = "a".repeat(245);
        String longEmail = longLocal + "@example.com"; // 257 ký tự
        Set<ConstraintViolation<User>> violations = validate(longEmail, "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email > 255 ký tự phải vi phạm @Size")
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Email — valid case
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("email hợp lệ → không vi phạm validation")
    void email_valid_shouldBeValid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "validPass8");
        assertThat(hasViolationOnField(violations, "email"))
                .as("email hợp lệ không được vi phạm")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Password — invalid cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("password null → vi phạm validation")
    void password_null_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", null);
        assertThat(hasViolationOnField(violations, "password"))
                .as("password null phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("password rỗng → vi phạm validation")
    void password_blank_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "");
        assertThat(hasViolationOnField(violations, "password"))
                .as("password rỗng phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("password chỉ có khoảng trắng → vi phạm validation")
    void password_whitespaceOnly_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "   ");
        assertThat(hasViolationOnField(violations, "password"))
                .as("password chỉ có whitespace phải vi phạm @NotBlank")
                .isTrue();
    }

    @Test
    @DisplayName("password 7 ký tự (dưới minimum 8) → vi phạm validation")
    void password_belowMinLength_shouldBeInvalid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "short12");
        assertThat(hasViolationOnField(violations, "password"))
                .as("password 7 ký tự phải vi phạm @Size(min=8)")
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Password — valid cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("password đúng 8 ký tự (minimum) → không vi phạm")
    void password_exactMinLength_shouldBeValid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "exactly8");
        assertThat(hasViolationOnField(violations, "password"))
                .as("password 8 ký tự (minimum) không được vi phạm")
                .isFalse();
    }

    @Test
    @DisplayName("password dài hơn minimum → không vi phạm")
    void password_longerThanMin_shouldBeValid() {
        Set<ConstraintViolation<User>> violations = validate("user@example.com", "a-longer-secure-password");
        assertThat(hasViolationOnField(violations, "password"))
                .as("password dài hơn minimum không được vi phạm")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Happy path: cả email và password hợp lệ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("email và password hợp lệ → User không có violation nào")
    void emailAndPassword_bothValid_noViolations() {
        Set<ConstraintViolation<User>> violations = validate("admin@ntdhtcct.com", "securePassword");
        assertThat(violations)
                .as("User hợp lệ không được có bất kỳ violation nào")
                .isEmpty();
    }
}
