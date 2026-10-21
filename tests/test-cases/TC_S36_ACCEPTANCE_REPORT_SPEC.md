# Đặc Tả Kiểm Thử Lập Phiếu Nghiệm Thu Theo Đợt Và Tự Tính Giá Trị (Module S-36)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-220 (S-36)** và các Subtasks:
- **NTDHTCT-268 (T-83)**: Tạo phiếu nghiệm thu theo từng đợt.
- **NTDHTCT-269 (T-84)**: Chọn hạng mục và nhập khối lượng nghiệm thu.
- **NTDHTCT-270 (T-85)**: Tự động tính giá trị nghiệm thu của phiếu.
Thuộc Epic: **E-06 - Nghiệm thu khối lượng và thanh quyết toán**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-268** | T-83 – Tạo phiếu nghiệm thu theo từng đợt | `TC-ACC-01` | Tạo phiếu nghiệm thu theo đợt với đầy đủ thông tin: số biên bản, tên phiếu, số đợt, ngày nghiệm thu | Lưu thành công xuống CSDL, khởi tạo đợt nghiệm thu mới, trả về HTTP 201 Created | **Passed** |
| **NTDHTCT-268** | T-83 – Tạo phiếu nghiệm thu theo từng đợt | `TC-ACC-02` | Tự động gợi ý số đợt tiếp theo (`next-period`) và mã biên bản định dạng `NT-{YYYY}-{period}` | Gợi ý đợt tăng tuần tự (Đợt 1, Đợt 2,...) không bị trùng lặp | **Passed** |
| **NTDHTCT-268** | T-83 – Tạo phiếu nghiệm thu theo từng đợt | `TC-ACC-03` | Chặn tạo phiếu khi trùng số đợt hoặc trùng số biên bản trong cùng một dự án | Báo lỗi xung đột: *"Số đợt nghiệm thu đã tồn tại"* hoặc *"Số biên bản đã tồn tại"* | **Passed** |
| **NTDHTCT-268** | T-83 – Tạo phiếu nghiệm thu theo từng đợt | `TC-ACC-04` | Cập nhật thông tin phiếu nghiệm thu (ngày nghiệm thu, trạng thái, ghi chú) | Dữ liệu được cập nhật chính xác, trạng thái chuyển đổi hợp lệ (`DRAFT`, `SUBMITTED`, `APPROVED`) | **Passed** |
| **NTDHTCT-269** | T-84 – Chọn hạng mục và nhập khối lượng nghiệm thu | `TC-ACC-05` | Chọn danh sách công việc WBS từ dự án đưa vào phiếu nghiệm thu | Nạp đúng mã công việc, tên hạng mục, đơn vị tính và đơn giá dự toán | **Passed** |
| **NTDHTCT-269** | T-84 – Chọn hạng mục và nhập khối lượng nghiệm thu | `TC-ACC-06` | Nhập và xác thực khối lượng nghiệm thu thực tế cho từng hạng mục | Chấp nhận số dương và số thập phân hợp lệ; lưu trữ thành công | **Passed** |
| **NTDHTCT-269** | T-84 – Chọn hạng mục và nhập khối lượng nghiệm thu | `TC-ACC-07` | Chặn nhập khối lượng nghiệm thu âm (`acceptedQuantity <= 0`) | Báo lỗi rõ ràng: *"Khối lượng nghiệm thu phải lớn hơn 0"* ở cả FE và BE | **Passed** |
| **NTDHTCT-269** | T-84 – Chọn hạng mục và nhập khối lượng nghiệm thu | `TC-ACC-08` | Chặn trùng lặp hạng mục WBS trong cùng một phiếu nghiệm thu | Báo lỗi: *"Hạng mục công việc đã tồn tại trong biên bản nghiệm thu"* | **Passed** |
| **NTDHTCT-270** | T-85 – Tự động tính giá trị nghiệm thu của phiếu | `TC-ACC-09` | Tự động tính thành tiền từng dòng: `amount = acceptedQuantity * unitPrice` theo thời gian thực | Hiển thị giá trị tức thì trên giao diện ngay khi thay đổi khối lượng nghiệm thu | **Passed** |
| **NTDHTCT-270** | T-85 – Tự động tính giá trị nghiệm thu của phiếu | `TC-ACC-10` | Tự động tính tổng giá trị toàn phiếu: `totalAmount = sum(items.amount)` | Tổng giá trị cập nhật chuẩn xác theo thời gian thực và được Backend kiểm chứng lại | **Passed** |
| **NTDHTCT-270** | T-85 – Tự động tính giá trị nghiệm thu của phiếu | `TC-ACC-11` | Độ chính xác dấu phẩy động (Floating point rounding) với số tiền lớn và số thập phân lẻ | Làm tròn chuẩn xác 2 chữ số thập phân, không sai lệch giá trị tiền tệ | **Passed** |
| **NTDHTCT-270** | T-85 – Tự động tính giá trị nghiệm thu của phiếu | `TC-ACC-12` | Thống kê lũy kế nghiệm thu dự án (`AcceptanceSummary`) | Tính đúng tổng số đợt, tổng giá trị đã nghiệm thu, phân loại theo trạng thái | **Passed** |

---

## 2. Minh Chứng Kết Quả Kiểm Thử Tự Động

### 2.1 Backend Tests (JUnit / Spring Boot)
Chạy lệnh kiểm thử tự động:
```bash
./mvnw.cmd test -Dtest=AcceptanceReportServiceTest
```
**Kết quả**:
- `testCreateAcceptanceReport_Success_AutoCalculatesTotalAmount`: Passed (T-83, T-84, T-85)
- `testCreateAcceptanceReport_DuplicatePeriodNumber_ThrowsException`: Passed (T-83)
- `testCreateAcceptanceReport_DuplicateReportNumber_ThrowsException`: Passed (T-83)
- `testCreateAcceptanceReport_NegativeQuantity_ThrowsException`: Passed (T-84)
- `testCalculateTotalAmount_MultipleItems_PrecisionAccurate`: Passed (T-85)
- `testGetNextPeriod_ReturnsCorrectPeriod`: Passed (T-83)
- **Tổng số**: 6/6 tests passed (BUILD SUCCESS).

### 2.2 Frontend Tests (Node.js Test Runner)
Chạy lệnh kiểm thử tự động:
```bash
node --test frontend/tests/acceptance.test.js
```
**Kết quả**:
- `TC-ACC-01: T-83 - Kiểm tra tính hợp lệ khi tạo phiếu nghiệm thu theo đợt`: Passed
- `TC-ACC-02: T-83 - Báo lỗi khi thiếu số biên bản, ngày nghiệm thu hoặc đợt <= 0`: Passed
- `TC-ACC-03: T-84 - Chọn hạng mục và xác thực khối lượng nghiệm thu`: Passed
- `TC-ACC-04: T-84 - Báo lỗi khi khối lượng nghiệm thu âm hoặc bằng 0 hoặc trùng hạng mục`: Passed
- `TC-ACC-05: T-85 - Tự động tính chính xác giá trị nghiệm thu từng dòng`: Passed
- `TC-ACC-06: T-85 - Giá trị nghiệm thu trả về 0 khi khối lượng hoặc đơn giá âm`: Passed
- `TC-ACC-07: T-85 - Tự động tính tổng giá trị nghiệm thu của toàn phiếu`: Passed
- `TC-ACC-08: T-85 - Tổng giá trị an toàn khi danh sách rỗng hoặc có phần tử không hợp lệ`: Passed
- **Tổng số**: 8/8 tests passed.

---

## 3. Chi Tiết Kịch Bản Thao Tác Nghiệp Vụ

### Kịch bản 1: Tạo phiếu nghiệm thu Đợt 1 và Chọn hạng mục (T-83, T-84)
1. Người dùng vào menu **Nghiệm thu khối lượng** (`/acceptance`).
2. Chọn dự án cần nghiệm thu.
3. Nhấn **+ Tạo phiếu nghiệm thu**:
   - Hệ thống tự điền số đợt: `1`, mã biên bản gợi ý: `NT-2026-001`.
   - Chọn ngày nghiệm thu và trạng thái `Dự thảo`.
4. Nhấn **+ Chọn hạng mục từ WBS**:
   - Modal hiển thị cây công việc/hạng mục của dự án kèm đơn vị tính và đơn giá hợp đồng.
   - Chọn các hạng mục: *"Bê tông dầm sàn"* (ĐVT: m3, Đơn giá: 1,350,000) và *"Cốt thép dầm sàn"* (ĐVT: tấn, Đơn giá: 18,200,000).
5. Nhập khối lượng nghiệm thu cho từng hạng mục:
   - *"Bê tông dầm sàn"*: `50` m3
   - *"Cốt thép dầm sàn"*: `12.5` tấn.

### Kịch bản 2: Tự động tính thành tiền và tổng giá trị phiếu (T-85)
1. Bảng nghiệm thu lập tức tự động nhân theo thời gian thực:
   - Thành tiền dòng 1: `50 * 1,350,000 = 67,500,000 VNĐ`.
   - Thành tiền dòng 2: `12.5 * 18,200,000 = 227,500,000 VNĐ`.
2. Hộp **Tổng giá trị nghiệm thu phiếu** hiển thị: `295,000,000 VNĐ`.
3. Nhấn **Lưu phiếu nghiệm thu**: Dữ liệu gửi lên backend được lưu trọn vẹn và cập nhật vào danh sách các đợt nghiệm thu của công trình.
