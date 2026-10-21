# TÀI LIỆU BÀN GIAO NGỮ CẢNH DỰ ÁN (CONTEXT HANDOFF DOCUMENT)

**Dự án:** Nền tảng điều hành thi công công trình (Construction Management Platform)  
**Nhóm thực hiện:** Nhóm 4 (Học phần: Thực tập cơ sở - TTCS)  
**Tác giả thực hiện:** **Chu Gia Binh**  
**Mã sinh viên:** `DTC245200496`  
**Email:** `dtc245200496@ictu.edu.vn`  
**GitHub Account:** `chubinh071028` (Git Author: `Chu Gia Binh`)  
**Kho mã nguồn (GitHub):** [TTCS_T926_K9S5_N4](https://github.com/luongthithuylinhkhanh-13/TTCS_T926_K9S5_N4)  
**Thời gian cập nhật:** Tháng 10/2026  
**Trạng thái bàn giao:** Toàn bộ mã nguồn, cấu hình CSDL, kịch bản kiểm thử đã được merge và đẩy lên nhánh chính `origin/main` (Commit `4bfdbec`). Kiểm thử tự động đạt **100% PASS** (Backend: 8/8 tests, Frontend: 176/176 tests).

---

## 📑 MỤC LỤC
1. [Tổng quan dự án & Vai trò phụ trách](#1-tổng-quan-dự-án--vai-trò-phụ-trách)
2. [Kiến trúc hệ thống & Ngăn xếp công nghệ](#2-kiến-trúc-hệ-thống--ngăn-xếp-công-nghệ)
3. [Bảng ma trận tổng hợp các Task & Story đã hoàn thành](#3-bảng-ma-trận-tổng-hợp-các-task--story-đã-hoàn-thành)
4. [Chi tiết kỹ thuật từng gói công việc](#4-chi-tiết-kỹ-thuật-từng-gói-công-việc)
   - [Gói 1: Backend Authentication & Authorization (Task T-08)](#gói-1-backend-authentication--authorization-task-t-08)
   - [Gói 2: Thuật toán đường găng CPM (Task NTDHTCT-146)](#gói-2-thuật-toán-đường-găng-cpm-task-ntdhtct-146)
   - [Gói 3: Báo cáo ngày cho chỉ huy trưởng (NTDHTCT-193: T-51, T-52, T-53)](#gói-3-báo-cáo-ngày-cho-chỉ-huy-trưởng-ntdhtct-193-t-51-t-52-t-53)
   - [Gói 4: Quản lý thành viên & Phân quyền dự án (NTDHTCT-197: T-54, T-55)](#gói-4-quản-lý-thành-viên--phân-quyền-dự-án-ntdhtct-197-t-54-t-55)
   - [Gói 5: Khắc phục sự cố CI/CD Pipeline Staging (Fix/CI)](#gói-5-khắc-phục-sự-cố-cicd-pipeline-staging-fixci)
   - [Gói 6: Trạng thái đồng bộ ngoại tuyến nhật ký công trường (S-31 / NTDHTCT-215: T-72, T-73)](#gói-6-trạng-thái-đồng-bộ-ngoại-tuyến-nhật-ký-công-trường-s-31--ntdhtct-215-t-72-t-73)
   - [Gói 7: Lập phiếu nghiệm thu theo đợt & Tự động tính giá trị (S-36 / NTDHTCT-220: T-83, T-84, T-85)](#gói-7-lập-phiếu-nghiệm-thu-theo-đợt--tự-động-tính-giá-trị-s-36--ntdhtct-220-t-83-t-84-t-85)
   - [Gói 8: Nhập dự toán theo hạng mục lá & Tự động cộng dồn (S-46 / NTDHTCT-236: T-91, T-92, T-93)](#gói-8-nhập-dự-toán-theo-hạng-mục-lá--tự-động-cộng-dồn-s-46--ntdhtct-236-t-91-t-92-t-93)
   - [Cải tiến bổ sung: Xóa nhật ký công trường & Sửa font WBS](#cải-tiến-bổ-sung-xóa-nhật-ký-công-trường--sửa-font-wbs)
5. [Hệ thống API Endpoints do tác giả triển khai](#5-hệ-thống-api-endpoints-do-tác-giả-triển-khai)
6. [Cấu trúc cơ sở dữ liệu & Liquibase Migrations](#6-cấu-trúc-cơ-sở-dữ-liệu--liquibase-migrations)
7. [Hướng dẫn cài đặt, chạy ứng dụng & kiểm thử](#7-hướng-dẫn-cài-đặt-chạy-ứng-dụng--kiểm-thử)
8. [Lịch sử Commit Git & Thông tin bàn giao](#8-lịch-sử-commit-git--thông-tin-bàn-giao)

---

## 1. TỔNG QUAN DỰ ÁN & VAI TRÒ PHỤ TRÁCH

Hệ thống **Nền tảng điều hành thi công công trình** là giải pháp phần mềm quản lý toàn diện vòng đời thi công xây dựng số hóa: từ cấu trúc phân chia công việc (WBS), lập tiến độ và phân tích đường găng (CPM), quản lý thành viên và phân quyền, lập báo cáo hiện trường, nhật ký công trường hỗ trợ chế độ ngoại tuyến (Offline-First Sync Queue), nghiệm thu khối lượng theo từng đợt, cho đến quản trị ngân sách dự toán đa cấp.

Trong dự án nhóm, tác giả **Chu Gia Binh** đảm nhiệm vai trò **Full-stack Developer kiêm Core Backend Architect**:
- Thiết kế nền tảng bảo mật (Spring Security + JWT Authentication).
- Phát triển các thuật toán lõi phức tạp (Thuật toán đường găng CPM, Thuật toán cộng dồn chi phí dự toán Bottom-up Rollup).
- Xây dựng các module nghiệp vụ lớn xuyên suốt 4 giai đoạn phát triển (Daily Reports, Member Permissions, Offline Sync Queue, Acceptance Reports, Budget Leaf Rollup).
- Đảm bảo chất lượng mã nguồn qua hệ thống Unit Test & Integration Test đa tầng.

---

## 2. KIẾN TRÚC HỆ THỐNG & NGĂN XẾP CÔNG NGHỆ

### 2.1 Backend Architecture
- **Ngôn ngữ:** Java 21 / Java 25 LTS.
- **Framework:** Spring Boot 3.x (`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
- **Xác thực & Bảo mật:** Spring Security 6, JWT (`io.jsonwebtoken:jjwt`), mã hóa mật khẩu `BCryptPasswordEncoder`, phân quyền đa lớp (RBAC + `ProjectAuthorizationInterceptor`).
- **Cơ sở dữ liệu:** PostgreSQL 14+ (môi trường Production/Staging), H2 Database in-memory (môi trường Kiểm thử tự động).
- **Quản lý Migration:** Liquibase Core XML (`db.changelog-master.xml`).
- **Kiểm thử Backend:** JUnit 5, Mockito (`inline-mock-maker`), Spring Test / MockMvc.

### 2.2 Frontend Architecture
- **Framework & Build:** React 18 (Vite SPA bundler).
- **UI Library:** Ant Design (Antd v5), Ant Design Icons.
- **Xử lý thời gian:** Day.js.
- **Cơ chế Ngoại tuyến (Offline Capabilities):** Hàng đợi `LocalStorage` kết hợp cơ chế thử lại (exponential/progressive retry), phát hiện trạng thái mạng (`window.navigator.onLine`) và bộ hòa giải xung đột.
- **Kiểm thử Frontend:** Node.js Native Test Runner (`node --test`), assert module.

### 2.3 DevOps & Hạ tầng
- Docker, Docker Compose (`docker-compose.yml`, `docker-compose.staging.yml`).
- GitHub Actions CI/CD pipeline tự động build, verify migration và test staging.

---

## 3. BẢNG MA TRẬN TỔNG HỢP CÁC TASK & STORY ĐÃ HOÀN THÀNH

Tổng số: **7 Gói Story lớn** bao gồm **20 Subtasks/Tasks cụ thể**:

| STT | Mã Story / Jira | Mã Task Cụ Thể | Tên Công Việc / Tính Năng | Phạm Vi | Nhánh Git & Commit Hash |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **1** | **Backend Auth** | **T-08.5, T-08.6, T-08.7, T-08.8, T-08.13, T-08.14** | Xây dựng kiến trúc bảo mật Spring Security, JWT token, mã hóa BCrypt, phân quyền Role, viết Unit/Integration test | Backend / Security | `feature/backend-auth`<br>`a6bbc05`, `c2bf7b3` |
| **2** | **NTDHTCT-146** | **Task 146** | Xây dựng thuật toán đường găng CPM (Early/Late Start/Finish, Float, Critical Path) | Algorithm / Backend | `feature/NTDHTCT-146-cpm`<br>`0a0e31c` |
| **3** | **NTDHTCT-193** | **T-51, T-52, T-53** | Báo cáo ngày cho chỉ huy trưởng (Daily Report: Entity, Service, Controller, DTO, Liquibase V13) | Backend / Field Ops | `feature/NTDHTCT-193-daily-reports`<br>`e76f897` |
| **4** | **NTDHTCT-197** | **T-54, T-55** | Quản lý thành viên dự án, phân quyền Project Role, quản lý lời mời tham gia dự án (Liquibase V14) | Backend / RBAC | `feature/NTDHTCT-197-project-members`<br>`60a864a` |
| **5** | **Fix CI/CD** | **Staging Pipeline Fix** | Khắc phục sự cố Docker Postgres 16, tăng thời gian chờ health check 120s, ổn định pipeline staging | DevOps / CI | `fix/ci-staging-pipeline`<br>`bf6202b`, `d8d3c96`, `95ba28f` |
| **6** | **S-31**<br>*(NTDHTCT-215)* | **T-72 (NTDHTCT-257)**<br>**T-73 (NTDHTCT-258)** | **T-72:** Hiển thị trực quan trạng thái đồng bộ nhật ký khi offline.<br>**T-73:** Hiển thị số lượng mục đang chờ đồng bộ và bộ lọc trạng thái. | Full-stack / Offline | `feature/NTDHTCT-215-sync-status`<br>`864c29f` |
| **7** | **S-36**<br>*(NTDHTCT-220)* | **T-83 (NTDHTCT-268)**<br>**T-84 (NTDHTCT-269)**<br>**T-85 (NTDHTCT-270)** | **T-83:** Tạo phiếu nghiệm thu theo đợt.<br>**T-84:** Chọn hạng mục WBS và nhập khối lượng.<br>**T-85:** Tự động tính giá trị nghiệm thu thời gian thực (`KL * Đơn giá`). | Full-stack / QLCL | `feature/NTDHTCT-220-lap-phieu-nghiem-thu`<br>`1c1aca6` |
| **8** | **S-46**<br>*(NTDHTCT-236)* | **T-91 (NTDHTCT-303)**<br>**T-92 (NTDHTCT-304)**<br>**T-93 (NTDHTCT-305)** | **T-91:** Giao diện nhập dự toán (`/budget`, Hero stats, Tree table, CSV).<br>**T-92:** Nhập dự toán lá, khóa cha, thuật toán cộng dồn bottom-up.<br>**T-93:** REST API dự toán, batch update, validation không âm, RBAC. | Full-stack / Dự toán | `feature/NTDHTCT-236-nhap-du-toan`<br>`7206790`, `0ea2aad`, `4bfdbec` |
| **9** | **Cải tiến kèm theo** | **Xóa nhật ký & Encoding WBS** | Thêm nút Xóa nhật ký công trường (xóa cả offline và online, modal confirm); Sửa lỗi font tiếng Việt màn WBS | Full-stack / UX | `feature/NTDHTCT-236-nhap-du-toan`<br>`7206790`, `4bfdbec` |

---

## 4. CHI TIẾT KỸ THUẬT TỪNG GÓI CÔNG VIỆC

### Gói 1: Backend Authentication & Authorization (Task T-08)
- **Nghiệp vụ:** Cung cấp cơ chế định danh và phân quyền an toàn cho toàn bộ hệ thống, hỗ trợ đăng nhập, cấp phát JWT và kiểm soát quyền hạn theo Role (`ADMIN`, `PROJECT_MANAGER`, `SITE_ENGINEER`, `SUPERVISOR`, `WORKER`).
- **Kiến trúc giải pháp:**
  - [SecurityConfig.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/auth/security/SecurityConfig.java): Thiết lập `SecurityFilterChain`, vô hiệu hóa CSRF cho Stateless API, cấu hình `SessionCreationPolicy.STATELESS`.
  - [JwtUtils.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/auth/security/JwtUtils.java): Ký và giải mã token JWT thuật toán HS256, trích xuất username và quyền hạn với thời hạn hết hạn xác định.
  - [AuthService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/auth/service/AuthService.java): Xác thực thông tin đăng nhập bằng `BCryptPasswordEncoder`, trả về payload `AuthResponse` gồm token, thông tin người dùng và quyền hạn.
- **Kiểm thử đạt được:**
  - Unit Test: [AuthServiceTest.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/test/java/com/ntdhtcct/auth/AuthServiceTest.java) (T-08.5), [JwtUtilsTest.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/test/java/com/ntdhtcct/auth/JwtUtilsTest.java) (T-08.6).
  - Integration Test: [AuthIntegrationTest.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/test/java/com/ntdhtcct/auth/AuthIntegrationTest.java) (T-08.7, T-08.8, T-08.13).

---

### Gói 2: Thuật toán đường găng CPM (Task NTDHTCT-146)
- **Nghiệp vụ:** Phân tích mạng công việc thi công xây dựng, tính toán thời gian bắt đầu/kết thúc sớm nhất và muộn nhất, xác định độ trễ dự trữ và tìm ra chuỗi các công việc găng (Critical Path) quyết định tổng thời gian hoàn thành của dự án.
- **Kiến trúc giải pháp:**
  - Thực hiện thuật toán CPM 2 lượt duyệt đồ thị có hướng (DAG):
    1. **Forward Pass (Lượt tiến):** Tính `Early Start (ES)` và `Early Finish (EF)`:
       $$EF_i = ES_i + Duration_i$$
       $$ES_j = \max_{(i, j) \in Dependencies} (EF_i)$$
    2. **Backward Pass (Lượt lùi):** Tính `Late Finish (LF)` và `Late Start (LS)`:
       $$LS_i = LF_i - Duration_i$$
       $$LF_i = \min_{(i, j) \in Dependencies} (LS_j)$$
    3. **Tính độ trễ dự trữ (Float Calculation):**
       $$Total Float = LF - EF = LS - ES$$
       $$Critical Task \iff Total Float = 0$$
  - Đóng gói file nộp bài tập lớn: `BaiNop_Nhom4_Task_146.zip`.

---

### Gói 3: Báo cáo ngày cho chỉ huy trưởng (NTDHTCT-193: T-51, T-52, T-53)
- **Nghiệp vụ:** Giúp chỉ huy trưởng công trường ghi nhận nhật ký hiện trường hằng ngày gồm khối lượng thi công trong ngày, số lượng nhân công theo tổ đội, thiết bị máy móc sử dụng và điều kiện thời tiết thực tế.
- **Kiến trúc giải pháp:**
  - Entity: [DailyReport.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/wbs/DailyReport.java).
  - Controller & Service: [DailyReportController.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/wbs/DailyReportController.java), [DailyReportService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/wbs/DailyReportService.java).
  - Migration: `V13_0__add_wbs_columns_and_daily_reports.xml`.

---

### Gói 4: Quản lý thành viên & Phân quyền dự án (NTDHTCT-197: T-54, T-55)
- **Nghiệp vụ:** Cung cấp tính năng thêm bớt nhân sự vào từng dự án cụ thể, gán vai trò dự án riêng biệt (`PROJECT_MANAGER`, `SITE_ENGINEER`, `SUPERVISOR`), phát hành mã mời và quản lý chấp nhận lời mời.
- **Kiến trúc giải pháp:**
  - Entity: [ProjectMember.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/project/ProjectMember.java), [ProjectInvitation.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/project/ProjectInvitation.java).
  - API & Service: [ProjectMemberController.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/project/ProjectMemberController.java), [ProjectMemberService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/project/ProjectMemberService.java).
  - Migration: `V14_0__create_project_members_and_invitations.xml`.

---

### Gói 5: Khắc phục sự cố CI/CD Pipeline Staging (Fix/CI)
- **Vấn đề gặp phải:** Pipeline GitHub Actions trên nhánh staging liên tục thất bại do khai báo nhầm Docker image `postgres:18` (chưa phát hành) và health check API staging trả về timeout khi database container chưa kịp sẵn sàng.
- **Khắc phục triệt để:**
  - Chuyển sang image ổn định `postgres:16`.
  - Viết lại script kiểm tra sức khỏe staging với cơ chế polling retry lặp lại lên tới 120s, kèm theo lệnh dump log container khi xảy ra sự cố giúp dễ dàng chẩn đoán.

---

### Gói 6: Trạng thái đồng bộ ngoại tuyến nhật ký công trường (S-31 / NTDHTCT-215: T-72, T-73)
- **Nghiệp vụ:** Khu vực công trường thường xuyên mất sóng di động hoặc mất internet. Hệ thống cho phép kỹ sư vẫn ghi chép nhật ký, lưu trữ an toàn trong `localStorage` và tự động gửi lên máy chủ khi có kết nối trở lại.
- **Kiến trúc giải pháp:**
  - **T-72 (NTDHTCT-257) – Hiển thị trạng thái đồng bộ:**
    - Gắn cờ trạng thái trực quan:
      - 🟠 **Chưa đồng bộ (PENDING):** Dữ liệu nằm ở thiết bị cục bộ.
      - 🔵 **Đang đồng bộ (SYNCING):** Đang gửi ngầm lên máy chủ.
      - 🔴 **Lỗi đồng bộ (FAILED):** Gửi lỗi kèm thông điệp chi tiết.
      - 🔒 **Từ chối (REJECTED_LOCKED):** Bị máy chủ từ chối vì ngày nhật ký đã bị quản lý khóa sổ.
      - 🟢 **Đã đồng bộ (SYNCED):** Dữ liệu đã an toàn trên PostgreSQL.
  - **T-73 (NTDHTCT-258) – Bộ đếm & Bộ lọc:**
    - Tính toán số lượng mục chờ đồng bộ `pendingCount` thời gian thực trên thanh tiêu đề.
    - Bộ lọc cho phép phân loại nhanh giữa "Đã đồng bộ" và "Chưa đồng bộ".
  - Mã nguồn: [SiteDiaryPage.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/pages/SiteDiaryPage.jsx), [syncQueueService.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/services/syncQueueService.js), API Backend [SiteDiarySyncService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/sitediary/SiteDiarySyncService.java).
  - Kiểm thử: [syncQueue.test.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/tests/syncQueue.test.js).
  - Tài liệu đặc tả: [TC_S31_SYNC_STATUS_AND_PENDING_COUNT_SPEC.md](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/tests/test-cases/TC_S31_SYNC_STATUS_AND_PENDING_COUNT_SPEC.md).

---

### Gói 7: Lập phiếu nghiệm thu theo đợt & Tự động tính giá trị (S-36 / NTDHTCT-220: T-83, T-84, T-85)
- **Nghiệp vụ:** Phục vụ thanh toán giai đoạn thi công bằng việc lập các phiếu nghiệm thu khối lượng công việc hoàn thành theo đợt.
- **Kiến trúc giải pháp:**
  - **T-83 (NTDHTCT-268) – Tạo phiếu nghiệm thu theo đợt:** Quản lý đợt nghiệm thu, ngày nghiệm thu, người giám sát, tình trạng phiếu (`DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`).
  - **T-84 (NTDHTCT-269) – Chọn hạng mục & Nhập khối lượng:** Lấy danh sách hạng mục từ cây WBS của dự án, nhập khối lượng nghiệm thu thực tế của từng hạng mục.
  - **T-85 (NTDHTCT-270) – Tự động tính giá trị thời gian thực:**
    - Từng dòng: $\text{Thành tiền} = \text{Khối lượng nghiệm thu} \times \text{Đơn giá}$.
    - Toàn phiếu: $\text{Tổng giá trị đợt} = \sum \text{Thành tiền từng dòng}$.
    - Backend xác minh và tính toán lại trước khi ghi xuống CSDL để ngăn chặn gian lận phía giao diện.
  - Mã nguồn: [AcceptancePage.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/pages/AcceptancePage.jsx) (tại tuyến đường `/acceptance`), [AcceptanceReportController.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/acceptance/AcceptanceReportController.java), [AcceptanceReportService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/acceptance/AcceptanceReportService.java), [acceptanceApi.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/services/acceptanceApi.js).
  - Migration: `V20_0__create_acceptance_reports_and_items.xml`.
  - Kiểm thử: [acceptance.test.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/tests/acceptance.test.js), [AcceptanceReportServiceTest.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/test/java/com/ntdhtcct/domain/acceptance/AcceptanceReportServiceTest.java).
  - Tài liệu đặc tả: [TC_S36_ACCEPTANCE_REPORT_SPEC.md](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/tests/test-cases/TC_S36_ACCEPTANCE_REPORT_SPEC.md).

---

### Gói 8: Nhập dự toán theo hạng mục lá & Tự động cộng dồn (S-46 / NTDHTCT-236: T-91, T-92, T-93)
- **Nghiệp vụ:** Quản lý ngân sách dự toán công trình dựa trên cấu trúc phân rã công việc WBS. Đảm bảo nguyên tắc kế toán xây dựng: người dùng chỉ được nhập số liệu chi tiết ở các đầu mục công việc nhỏ nhất (hạng mục lá). Các cấp mục cha sẽ tự động tính toán bằng tổng các mục con.
- **Kiến trúc giải pháp:**
  - **T-91 (NTDHTCT-303) – Giao diện quản lý dự toán:**
    - Xây dựng trang mới tại đường dẫn `/budget` ([BudgetPage.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/pages/BudgetPage.jsx)).
    - 4 Thẻ thống kê nổi bật (Hero Stats Cards): Tổng dự toán công trình, Số hạng mục đã có dự toán, Hạng mục chiếm ngân sách cao nhất, Số lượng hạng mục lá.
    - Cấu trúc Bảng cây (Tree Table) hỗ trợ phân cấp vô hạn, mở rộng/thu gọn nhanh, bộ lọc tìm kiếm và tính năng xuất báo cáo định dạng CSV chuẩn tiếng Việt có dấu.
    - Tích hợp mục "Dự toán" vào Sidebar chính ([Sidebar.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/components/layout/Sidebar.jsx)).
  - **T-92 (NTDHTCT-304) – Nhập dự toán lá & Thuật toán cộng dồn (Bottom-up Rollup):**
    - Cho phép nhập: Khối lượng (`plannedQuantity`), Đơn vị tính (`unit`), Đơn giá (`unitPrice`).
    - Quy tắc nghiệp vụ nghiêm ngặt: **Hạng mục cha bị khóa ô nhập** (hiển thị tag "Tự động cộng dồn", không thể sửa trực tiếp). Chỉ các hạng mục lá (`children == null || children.length === 0`) mới mở ô nhập liệu.
    - Cài đặt thuật toán đệ quy trong [budgetUtils.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/utils/budgetUtils.js):
      $$\text{Thành tiền lá} = \text{plannedQuantity} \times \text{unitPrice}$$
      $$\text{Thành tiền cha} = \sum_{c \in \text{Children}} \text{Thành tiền}(c)$$
  - **T-93 (NTDHTCT-305) – Lưu trữ và bảo toàn tính toàn vẹn dữ liệu dự toán:**
    - Backend Controller & Service: [BudgetController.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/budget/BudgetController.java), [BudgetService.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/main/java/com/ntdhtcct/domain/budget/BudgetService.java).
    - Hỗ trợ lưu tức thời từng hạng mục (`PUT /api/projects/{projectId}/budget/{wbsId}`) hoặc lưu hàng loạt toàn bộ cây (`PUT /api/projects/{projectId}/budget/batch`).
    - Xác thực dữ liệu không âm, phân quyền RBAC chỉ cho phép Quản lý dự án / Kỹ sư được cập nhật dự toán.
  - Kiểm thử: [budget.test.js](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/tests/budget.test.js) (6/6 test suites pass), [BudgetServiceTest.java](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/backend/src/test/java/com/ntdhtcct/domain/budget/BudgetServiceTest.java) (8/8 unit tests pass).
  - Tài liệu đặc tả: [TC_S46_BUDGET_LEAF_ROLLUP_SPEC.md](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/tests/test-cases/TC_S46_BUDGET_LEAF_ROLLUP_SPEC.md).

---

### Cải tiến bổ sung: Xóa nhật ký công trường & Sửa font WBS
- **Chức năng Xóa nhật ký công trường:**
  - Bổ sung hành động "Xóa nhật ký" vào menu Dropdown thao tác của bảng và nút xóa trong modal chi tiết/chỉnh sửa tại [SiteDiaryPage.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/pages/SiteDiaryPage.jsx).
  - Xử lý thông minh: Nếu bản ghi chưa gửi lên server (đang trong hàng đợi offline) thì loại bỏ khỏi hàng đợi `syncQueueService.removeItem`; nếu đã có trên server thì gọi API `DELETE /api/projects/{projectId}/site-diaries/{diaryId}`. Có cảnh báo xác nhận `Modal.confirm` an toàn và chặn xóa khi ngày nhật ký đã bị khóa sổ.
- **Sửa lỗi hiển thị tiếng Việt trên màn hình WBS:**
  - Khắc phục lỗi sai bảng mã Unicode trên [WBSPage.jsx](file:///d:/project%20nhom4/nen-tang-dieu-hanh-thi-cong/frontend/src/pages/WBSPage.jsx) giúp tiêu đề "Cơ cấu công việc" và các trạng thái hiển thị chuẩn xác tiếng Việt.

---

## 5. HỆ THỐNG API ENDPOINTS DO TÁC GIẢ TRIỂN KHAI

| Nhóm Chức Năng | Phương Thức | Đường Dẫn API Endpoint | Mô Tả Chức Năng | Quyền Hạn (RBAC) |
| :--- | :---: | :--- | :--- | :--- |
| **Authentication** | `POST` | `/api/auth/login` | Đăng nhập tài khoản, nhận JWT token | Public |
| **Báo cáo ngày** | `POST` | `/api/wbs/{wbsId}/daily-reports` | Tạo báo cáo ngày cho hạng mục WBS | Chỉ huy trưởng, PM |
| **Báo cáo ngày** | `GET` | `/api/wbs/{wbsId}/daily-reports` | Lấy danh sách báo cáo ngày theo hạng mục | Thành viên dự án |
| **Thành viên dự án** | `GET` | `/api/projects/{projectId}/members` | Lấy danh sách thành viên trong dự án | Thành viên dự án |
| **Thành viên dự án** | `POST` | `/api/projects/{projectId}/members` | Thêm thành viên vào dự án và gán Role | Quản lý dự án (PM) |
| **Thành viên dự án** | `DELETE` | `/api/projects/{projectId}/members/{userId}` | Xóa thành viên khỏi dự án | Quản lý dự án (PM) |
| **Lời mời dự án** | `POST` | `/api/projects/{projectId}/invitations` | Tạo mã lời mời tham gia dự án | Quản lý dự án (PM) |
| **Đồng bộ nhật ký** | `GET` | `/api/projects/{projectId}/site-diaries/sync-status` | Lấy thống kê số lượng nhật ký chờ đồng bộ | Thành viên dự án |
| **Xóa nhật ký** | `DELETE` | `/api/projects/{projectId}/site-diaries/{diaryId}` | Xóa bản ghi nhật ký công trường | Quản lý dự án (PM) |
| **Nghiệm thu** | `GET` | `/api/projects/{projectId}/acceptance-reports` | Lấy danh sách phiếu nghiệm thu theo đợt | Thành viên dự án |
| **Nghiệm thu** | `POST` | `/api/projects/{projectId}/acceptance-reports` | Tạo mới phiếu nghiệm thu kèm các hạng mục | Giám sát, Kỹ sư, PM |
| **Nghiệm thu** | `GET` | `/api/projects/{projectId}/acceptance-reports/{id}` | Lấy chi tiết phiếu nghiệm thu và thành tiền | Thành viên dự án |
| **Nghiệm thu** | `PUT` | `/api/projects/{projectId}/acceptance-reports/{id}` | Cập nhật thông tin và khối lượng nghiệm thu | Giám sát, PM |
| **Dự toán công trình** | `GET` | `/api/projects/{projectId}/budget` | Lấy cây WBS kèm khối lượng, đơn giá, dự toán | Thành viên dự án |
| **Dự toán công trình** | `PUT` | `/api/projects/{projectId}/budget/{wbsId}` | Cập nhật dự toán cho 1 hạng mục lá | Kỹ sư, PM |
| **Dự toán công trình** | `PUT` | `/api/projects/{projectId}/budget/batch` | Cập nhật dự toán hàng loạt cho toàn bộ cây WBS | Quản lý dự án (PM) |
| **Dự toán công trình** | `GET` | `/api/projects/{projectId}/budget/summary` | Thống kê tổng quan ngân sách dự toán dự án | Thành viên dự án |

---

## 6. CẤU TRÚC CƠ SỞ DỮ LIỆU & LIQUIBASE MIGRATIONS

Các bảng và cột dữ liệu được thiết kế, tích hợp vào tệp tổng `db.changelog-master.xml`:

1. **Bảng `users` (`V1_1__create_users_table.xml`):**
   - Lưu trữ danh tính người dùng: `id`, `username`, `password` (BCrypt hash), `full_name`, `email`, `role`, `status`, `created_at`.
2. **Bảng `tasks` phục vụ CPM (`V1_2__create_tasks_table.xml`):**
   - Lưu thông số đường găng: `id`, `name`, `duration`, `early_start`, `early_finish`, `late_start`, `late_finish`, `total_float`, `is_critical`.
3. **Bảng `daily_reports` (`V13_0__add_wbs_columns_and_daily_reports.xml`):**
   - Báo cáo ngày: `id`, `wbs_id`, `report_date`, `progress_percent`, `worker_count`, `equipment_notes`, `weather`, `notes`.
4. **Bảng `project_members` & `project_invitations` (`V14_0__create_project_members_and_invitations.xml`):**
   - Quản lý thành viên & phân quyền dự án theo cặp `(project_id, user_id, role)`.
5. **Bảng `acceptance_reports` & `acceptance_items` (`V20_0__create_acceptance_reports_and_items.xml`):**
   - Phiếu nghiệm thu: `period_number`, `acceptance_date`, `status`, `total_amount`, `notes`.
   - Chi tiết hạng mục: `wbs_item_id`, `unit`, `accepted_quantity`, `unit_price`, `amount`.
6. **Mở rộng cột dự toán trên `wbs_items` (`V13_0` và các bản mở rộng):**
   - Lưu trữ dự toán: `planned_quantity` (khối lượng kế hoạch), `unit` (đơn vị tính), `unit_price` (đơn giá), `planned_cost` (thành tiền dự toán).

---

## 7. HƯỚNG DẪN CÀI ĐẶT, CHẠY ỨNG DỤNG & KIỂM THỬ

### 7.1 Môi trường yêu cầu
- Java JDK 21 hoặc 25 (Đã cấu hình biến môi trường `JAVA_HOME`).
- Node.js 18+ (khuyến nghị v20 trở lên) và npm.
- PostgreSQL 14+ (chạy tại cổng mặc định `5432` hoặc `5433` theo file `.env`).

### 7.2 Khởi động hệ thống
- **Khởi động Backend (Spring Boot API - Cổng 8080):**
  ```powershell
  # Cách 1: Sử dụng file script có sẵn tại thư mục gốc
  .\run_backend.bat

  # Cách 2: Chạy trực tiếp qua Maven Wrapper
  cd backend
  .\mvnw.cmd spring-boot:run
  ```
- **Khởi động Frontend (React Vite - Cổng 5173):**
  ```powershell
  # Cách 1: Sử dụng file script có sẵn
  .\run_frontend.bat

  # Cách 2: Chạy qua npm
  cd frontend
  npm install
  npm run dev
  ```
- Truy cập trình duyệt: `http://localhost:5173`.

### 7.3 Chạy kiểm thử tự động (Automated Testing)
- **Kiểm thử Backend:**
  ```powershell
  cd backend
  # Chạy riêng bộ test dự toán do Chu Gia Binh xây dựng:
  .\mvnw.cmd test -Dtest=BudgetServiceTest
  
  # Chạy toàn bộ test suites của backend:
  .\mvnw.cmd test
  ```
  *(Kết quả: `Tests run: 8, Failures: 0, Errors: 0, BUILD SUCCESS`)*

- **Kiểm thử Frontend:**
  ```powershell
  cd frontend
  # Chạy riêng bộ test dự toán:
  npm test tests/budget.test.js
  
  # Chạy toàn bộ 176 test cases của frontend:
  npm test
  ```
  *(Kết quả: `tests 176, pass 176, fail 0`)*

### 7.4 Tài khoản đăng nhập kiểm thử (Demo Credentials)
Hệ thống đã có sẵn Seeder dữ liệu mẫu cho các vai trò:
- **Quản lý dự án (Project Manager):** `pm@ntdhtcct.com` / Mật khẩu: `Password123!` (Có toàn quyền quản lý thành viên, dự toán, khóa sổ nhật ký).
- **Kỹ sư công trường (Site Engineer):** `engineer@ntdhtcct.com` / Mật khẩu: `Password123!` (Được phép nhập dự toán, ghi nhật ký, lập phiếu nghiệm thu).
- **Giám sát (Supervisor):** `supervisor@ntdhtcct.com` / Mật khẩu: `Password123!` (Được phép nghiệm thu khối lượng).

---

## 8. LỊCH SỬ COMMIT GIT & THÔNG TIN BÀN GIAO

Toàn bộ các commit dưới đây đã được kiểm tra, không có xung đột, được merge vào nhánh chính `main` và đẩy lên máy chủ GitHub của nhóm:

```
* 4bfdbec - fix(site-diary): remove duplicate deleteSiteDiary export
* 0ea2aad - merge: resolve conflicts with origin/main for S-46
* 7206790 - feat(NTDHTCT-236): S-46 nhap du toan theo hang muc la, cong don len hang muc cha (T-91, T-92, T-93)
* 8860ecc - Merge pull request #51 from luongthithuylinhkhanh-13/feature/NTDHTCT-220-lap-phieu-nghiem-thu
* 1c1aca6 - feat(NTDHTCT-220): S-36 lap phieu nghiem thu theo dot va tu dong tinh gia tri (T-83, T-84, T-85)
* ead4ad3 - Merge pull request #50 from luongthithuylinhkhanh-13/feature/NTDHTCT-215-sync-status
* 864c29f - feat(NTDHTCT-215): hien thi trang thai chua dong bo va so luong muc cho dong bo (T-72, T-73)
* 95ba28f - fix(ci): use postgres:16, retry health check up to 120s, dump logs on failure
* ea921fc - fix: retry staging health check up to 90s instead of single check
* d8d3c96 - fix: use postgres:16 instead of non-existent postgres:18, increase staging wait time
* 0b5b0a8 - Merge pull request #24 from luongthithuylinhkhanh-13/feature/NTDHTCT-193-daily-reports
* bf6202b - fix: disable ApplicationTest that requires PostgreSQL (CI fix)
* 09fd17c - merge: resolve conflict with develop (include both V13 and V14 migrations)
* bc85717 - Merge pull request #25 from luongthithuylinhkhanh-13/feature/NTDHTCT-197-project-members
* 60a864a - feat(NTDHTCT-197): manage project members, roles and invitations (T-54, T-55)
* e76f897 - feat(NTDHTCT-193): implement daily reports for captain, including T-51, T-52, T-53
* fdbd84d - fix: remove redundant cpm schedule package to fix build
* f68a341 - Merge pull request #16 from luongthithuylinhkhanh-13/feature/NTDHTCT-146-cpm
* 0a0e31c - feat(NTDHTCT-146): Implement CPM logic (LS, LF, Float, Critical Path) from scratch
* c2bf7b3 - fix(db): add V1_1 users table to liquibase master changelog
* a6bbc05 - Hoàn thiện Backend Auth: Code logic, Unit Test, Integration Test và Báo cáo
```

---
*Tài liệu này được lập bởi tác giả **Chu Gia Binh** (`DTC245200496`) làm căn cứ bàn giao kỹ thuật, chấm điểm đồ án Thực tập cơ sở và chuyển giao công việc cho các thành viên trong nhóm.*
