package com.ntdhtcct.auth.annotation;

import java.lang.annotation.*;

/**
 * T-04.8: Annotation cấu hình quyền hạn truy cập theo từng Route / Controller / Method.
 * Định nghĩa danh sách vai trò trong dự án được phép thực hiện hành vi.
 *
 * Ví dụ:
 *   @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
 *   @RequireProjectRole({"SITE_ENGINEER"})
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireProjectRole {

    /**
     * Danh sách mã vai trò được phép truy cập (ví dụ: "ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER").
     * Mặc định rỗng nghĩa là chỉ cần là thành viên tích cực của dự án (bất kỳ role nào).
     */
    String[] value() default {};
}
