package com.ntdhtcct.entity;

/**
 * Định nghĩa danh mục mã vai trò chuẩn trong hệ thống điều hành thi công công trình.
 */
public enum RoleCode {
    ADMIN("Quản trị hệ thống"),
    PROJECT_MANAGER("Ban quản lý dự án"),
    CUSTOMER("Chủ đầu tư"),
    SITE_MANAGER("Chỉ huy trưởng công trường"),
    SITE_ENGINEER("Kỹ sư giám sát"),
    WORKER("Đội trưởng thi công"),
    STAFF("Kế toán chủ đầu tư"),
    VIEWER("Người xem / Kế toán (Legacy)");

    private final String description;

    RoleCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
