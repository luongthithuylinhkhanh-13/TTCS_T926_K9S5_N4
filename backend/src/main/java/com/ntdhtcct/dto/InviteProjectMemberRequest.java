package com.ntdhtcct.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InviteProjectMemberRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mã vai trò (roleCode) không được để trống")
    @Size(max = 50, message = "Mã vai trò không được vượt quá 50 ký tự")
    private String roleCode;

    public InviteProjectMemberRequest() {
    }

    public InviteProjectMemberRequest(String email, String roleCode) {
        this.email = email;
        this.roleCode = roleCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
