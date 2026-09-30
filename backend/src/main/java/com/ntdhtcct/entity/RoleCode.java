package com.ntdhtcct.entity;

/**
 * Định nghĩa danh mục mã vai trò chuẩn trong hệ thống điều hành thi công công trình.
 */
public enum RoleCode {
    ADMIN("Quản trị hệ thống"),
    PROJECT_MANAGER("Chỉ huy trưởng / Quản lý dự án"),
    SITE_ENGINEER("Kỹ sư công trường / Giám sát"),
    WORKER("Đội thi công / Thầu phụ"),
    VIEWER("Người xem / Kế toán");

    private final String description;

    RoleCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
