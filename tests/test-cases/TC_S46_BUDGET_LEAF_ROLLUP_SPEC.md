# Đặc Tả Kiểm Thử Nhập Dự Toán Theo Hạng Mục Lá Và Tự Động Cộng Dồn (Module S-46)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-236 (S-46: Nhập dự toán theo hạng mục lá, cộng dồn lên hạng mục cha)** và các Subtasks:
- **NTDHTCT-303 (T-91)**: Xây dựng giao diện nhập dự toán.
- **NTDHTCT-304 (T-92)**: Nhập dự toán cho từng hạng mục.
- **NTDHTCT-305 (T-93)**: Lưu và cập nhật dữ liệu dự toán.
Thuộc Epic: **E-09 - Dự toán, thực chi và vật tư**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-303** | T-91 – Xây dựng giao diện nhập dự toán | `TC-BUD-01` | Hiển thị cấu trúc cây dự toán WBS theo cấp bậc, phân biệt trực quan hạng mục lá và hạng mục cha | Hạng mục lá hiển thị Tag xanh lá `Hạng mục lá`, hạng mục cha hiển thị Tag xanh dương `Cộng dồn tự động` | **Passed** |
| **NTDHTCT-303** | T-91 – Xây dựng giao diện nhập dự toán | `TC-BUD-02` | Thống kê số liệu tổng quan dự toán (Hero Cards): tổng kinh phí dự toán, số lượng hạng mục lá đã nhập, tỷ lệ hoàn thành | Hiển thị chính xác các chỉ số tổng hợp, tự động tính toán theo thời gian thực | **Passed** |
| **NTDHTCT-303** | T-91 – Xây dựng giao diện nhập dự toán | `TC-BUD-03` | Tính năng lọc (Tất cả / Hạng mục lá / Hạng mục cha / Chưa có dự toán) và tìm kiếm theo mã WBS, tên công việc | Lọc nhanh và chính xác các node trên bảng cây | **Passed** |
| **NTDHTCT-303** | T-91 – Xây dựng giao diện nhập dự toán | `TC-BUD-04` | Xuất bảng dự toán chi tiết dự án ra định dạng CSV (hỗ trợ Excel tiếng Việt UTF-8 BOM) | File tải về đúng tên dự án, đầy đủ khối lượng, đơn giá và công thức cộng dồn | **Passed** |
| **NTDHTCT-304** | T-92 – Nhập dự toán cho từng hạng mục | `TC-BUD-05` | Tính đúng thành tiền dự toán của hạng mục lá: `Thành tiền = Khối lượng * Đơn giá` | Tự động tính toán tức thì trên form và bảng dữ liệu, làm tròn chuẩn xác 2 chữ số thập phân | **Passed** |
| **NTDHTCT-304** | T-92 – Nhập dự toán cho từng hạng mục | `TC-BUD-06` | **Quy tắc bắt buộc S-46:** Chặn nhập dự toán trực tiếp cho hạng mục cha (Parent node) | Nút thao tác trên hạng mục cha bị vô hiệu hóa kèm Tooltip cảnh báo, Backend ném lỗi HTTP 400 nếu cố tình gọi API | **Passed** |
| **NTDHTCT-304** | T-92 – Nhập dự toán cho từng hạng mục | `TC-BUD-07` | **Cơ chế Rollup Bottom-Up:** Tự động cộng dồn thành tiền từ các hạng mục lá lên các hạng mục cha | Giá trị hạng mục cha = Tổng thành tiền các hạng mục con, cập nhật đệ quy lên tất cả các cấp tổ tiên và toàn dự án | **Passed** |
| **NTDHTCT-304** | T-92 – Nhập dự toán cho từng hạng mục | `TC-BUD-08` | Cộng dồn đa cấp phức tạp (Gốc -> Giai đoạn -> Nhóm công việc -> Hạng mục lá) | Số liệu cộng dồn toàn dự án chuẩn xác 100%, không sai lệch số học | **Passed** |
| **NTDHTCT-305** | T-93 – Lưu và cập nhật dữ liệu dự toán | `TC-BUD-09` | Lưu và cập nhật thành công dự toán của một hạng mục lá qua API `PUT /api/projects/{projectId}/budget/{itemId}` | Dữ liệu `total_volume`, `unit`, `unit_price` lưu vào CSDL, API trả về cây dự toán với giá trị cộng dồn mới | **Passed** |
| **NTDHTCT-305** | T-93 – Lưu và cập nhật dữ liệu dự toán | `TC-BUD-10` | Cập nhật dự toán hàng loạt (Batch update) qua API `PUT /api/projects/{projectId}/budget/batch` | Hỗ trợ chế độ "Nhập nhanh trên bảng" và lưu đồng loạt nhiều hạng mục lá thành công | **Passed** |
| **NTDHTCT-305** | T-93 – Lưu và cập nhật dữ liệu dự toán | `TC-BUD-11` | Kiểm tra tính hợp lệ dữ liệu: từ chối khối lượng âm, đơn giá âm, đơn vị tính > 50 ký tự | Chặn submit tại Form và trả về HTTP 400 Bad Request rõ ràng tại Backend | **Passed** |
| **NTDHTCT-305** | T-93 – Lưu và cập nhật dữ liệu dự toán | `TC-BUD-12` | Bắt buộc phải có đơn vị tính khi đã khai báo khối lượng `> 0` | Báo lỗi: *"Vui lòng nhập đơn vị tính khi đã khai báo khối lượng"* | **Passed** |
| **NTDHTCT-305** | T-93 – Lưu và cập nhật dữ liệu dự toán | `TC-BUD-13` | Cấu hình phân quyền RBAC cho tuyến đường `/api/projects/*/budget/**` | Chỉ `ADMIN`, `PROJECT_MANAGER`, `SITE_ENGINEER` được quyền chỉnh sửa/lưu; `VIEWER`, `WORKER` có quyền xem | **Passed** |

---

## 2. Minh Chứng Kết Quả Kiểm Thử Tự Động

### 2.1 Backend Tests (JUnit 5 / Spring Boot)
Chạy lệnh kiểm thử tự động:
```bash
./mvnw.cmd test -Dtest=BudgetServiceTest
```
**Kết quả**:
- `testLeafBudgetCalculation_Success`: Passed (T-92)
- `testRollupToParent_TwoLevels`: Passed (T-92)
- `testMultiLevelRollup`: Passed (T-92)
- `testInputBudgetOnParentItem_ThrowsException`: Passed (T-92, T-93)
- `testUpdateBudgetItem_Leaf_Success`: Passed (T-93)
- `testValidation_Errors`: Passed (T-93)
- `testBatchUpdateBudget_Success`: Passed (T-92, T-93)
- `testGetBudgetSummary_CalculatesCorrectStats`: Passed (T-91)
- **Tổng số**: 8/8 tests passed (BUILD SUCCESS).

### 2.2 Frontend Tests (Node.js Test Runner)
Chạy lệnh kiểm thử tự động:
```bash
node --test frontend/tests/budget.test.js
```
**Kết quả**:
- `TC-BUD-01: T-92 - Tính chính xác thành tiền dự toán = khối lượng * đơn giá cho hạng mục lá`: Passed
- `TC-BUD-02: T-92 - Thành tiền trả về 0 khi khối lượng hoặc đơn giá chưa nhập hoặc bằng 0`: Passed
- `TC-BUD-03: T-92 - Tự động cộng dồn (rollup) từ các hạng mục lá lên hạng mục cha`: Passed
- `TC-BUD-04: T-92 - Tự động cộng dồn cây đa cấp (Cụ -> Ông -> Cha -> Con lá)`: Passed
- `TC-BUD-05: T-82 & T-93 - Kiểm tra tính hợp lệ của dữ liệu dự toán`: Passed
- `TC-BUD-06: T-91 - Tính toán chính xác thống kê tổng quan dự toán (Budget Summary)`: Passed
- **Toàn bộ test suite**: 75/75 tests passed (0 fail).

---

## 3. Chi Tiết Kịch Bản Thao Tác Nghiệp Vụ

### Kịch bản 1: Mở giao diện Quản lý dự toán và Xem tổng quan (T-91)
1. Người dùng vào menu **Dự toán** (`/budget`) trên thanh điều hướng bên trái.
2. Chọn dự án cần quản lý: ví dụ *"DA-001 - Xây dựng Chung cư Green Tower"*.
3. Hệ thống hiển thị 4 thẻ thống kê:
   - **Tổng dự toán công trình** (VNĐ)
   - **Hạng mục đã có dự toán** (ví dụ: 12/16 hạng mục, kèm thanh tiến độ %)
   - **Nhóm hạng mục cộng dồn** (số lượng nhóm cha)
   - **Tổng số hạng mục WBS**
4. Bảng phân cấp cây WBS hiển thị toàn bộ hạng mục:
   - Hạng mục cha: Hiển thị icon thư mục, Tag `Cộng dồn tự động`, ô Thành tiền in đậm số tiền cộng dồn từ nhánh con, ô Thao tác có nút bị vô hiệu hóa kèm Tooltip: *"Hạng mục cha tự động cộng dồn từ các hạng mục con, không thể nhập trực tiếp"*.
   - Hạng mục lá: Hiển thị icon văn bản, Tag `Hạng mục lá`, nút **Nhập dự toán**.

### Kịch bản 2: Khai báo dự toán cho hạng mục lá và Tự động cộng dồn (T-92, T-93)
1. Tại một hạng mục lá (ví dụ: *"Bê tông dầm sàn"*), người dùng nhấn **Nhập dự toán**.
2. Modal khai báo hiển thị:
   - Nhập Khối lượng: `50`
   - Chọn Đơn vị tính: `m3`
   - Nhập Đơn giá: `1,350,000`
3. Box ước tính thời gian thực hiển thị ngay: `67,500,000 VNĐ` kèm công thức chi tiết.
4. Nhấn **Lưu dự toán**:
   - Backend kiểm tra hợp lệ, xác nhận node là hạng mục lá và lưu vào CSDL.
   - Bảng cây lập tức cập nhật:
     - Dòng lá hiển thị: `50 m3`, đơn giá `1,350,000 đ`, thành tiền `67,500,000 đ`.
     - Hạng mục cha trực tiếp và các cấp cha cấp trên tự động cộng dồn tăng thêm `67,500,000 đ`.
     - Thẻ **Tổng dự toán công trình** ở trên cùng tự động tăng tương ứng.

### Kịch bản 3: Chế độ nhập nhanh trên bảng và Lưu hàng loạt (T-91, T-93)
1. Người dùng nhấn nút **Nhập nhanh trên bảng**.
2. Các ô Khối lượng, ĐVT, Đơn giá của tất cả hạng mục lá chuyển sang dạng ô nhập liệu (Input/Select).
3. Người dùng nhập nhanh nhiều dòng cùng lúc. Ô Thành tiền của từng dòng và thẻ cộng dồn phản ánh tức thì.
4. Nhấn **Lưu toàn bộ thay đổi**: Hệ thống gửi request batch lên backend, lưu toàn bộ dữ liệu và thông báo cập nhật thành công.
