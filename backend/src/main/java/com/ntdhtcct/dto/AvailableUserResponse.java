package com.ntdhtcct.dto;

import java.util.UUID;

/**
 * DTO đại diện cho tài khoản ACTIVE có thể thêm vào dự án.
 */
public record AvailableUserResponse(
        UUID id,
        String email,
        String fullName,
        String status,
        String systemRole,
        boolean isAlreadyMember
) {}
