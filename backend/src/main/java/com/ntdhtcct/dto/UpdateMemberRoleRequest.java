package com.ntdhtcct.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * T-04.4: DTO cập nhật vai trò của thành viên trong dự án.
 */
public class UpdateMemberRoleRequest {

    @NotBlank(message = "Tên vai trò mới (newRoleName) không được để trống")
    private String newRoleName;

    public UpdateMemberRoleRequest() {
    }

    public UpdateMemberRoleRequest(String newRoleName) {
        this.newRoleName = newRoleName;
    }

    public String getNewRoleName() {
        return newRoleName;
    }

    public void setNewRoleName(String newRoleName) {
        this.newRoleName = newRoleName;
    }
}
