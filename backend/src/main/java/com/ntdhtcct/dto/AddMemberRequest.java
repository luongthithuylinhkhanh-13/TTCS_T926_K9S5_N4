package com.ntdhtcct.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * T-04.4: DTO yêu cầu gán vai trò và thêm thành viên vào dự án.
 */
public class AddMemberRequest {

    @NotNull(message = "ID người dùng (userId) không được để trống")
    private UUID userId;

    @NotBlank(message = "Mã vai trò (roleCode) không được để trống")
    private String roleCode;

    public AddMemberRequest() {
    }

    public AddMemberRequest(UUID userId, String roleCode) {
        this.userId = userId;
        this.roleCode = roleCode;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
