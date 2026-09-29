# Đặc Tả Kiểm Thử Tiến Độ CPM: Tính Thời Điểm Khởi Sớm & Kết Thúc Sớm (Epic E-04 / Story S-08)

Tài liệu kiểm thử tương ứng với các thẻ công việc (Jira Tasks):
- **NTDHTCT-143**: `S-08 — Tính thời điểm khởi sớm và kết thúc sớm`
- **NTDHTCT-144**: `T-18 — Xây dựng công thức cho bốn loại quan hệ`
- **NTDHTCT-145**: `T-19 — Tính ES và EF`

---

## 1. Cơ Sở Lý Thuyết & Công Thức Toán Học (Subtask T-18 - NTDHTCT-144)

Trong phương pháp mạng công việc PDM (Precedence Diagramming Method) của phương pháp Đường găng CPM (Critical Path Method):
- Công việc tiền nhiệm (Predecessor $P$): Khởi sớm $ES_P$, Kết thúc sớm $EF_P = ES_P + Duration_P$.
- Công việc kế nhiệm (Successor $S$): Thời lượng $D_S \ge 0$, Khởi sớm $ES_S$, Kết thúc sớm $EF_S = ES_S + D_S$.
- Mối liên kết có độ trễ $Lag$ ($Lag \in \mathbb{R}$, dương là thời gian trễ trễ, âm là lead time/gối đầu sớm).

### Bốn loại quan hệ phụ thuộc:

| Loại Quan Hệ | Tên Tiếng Anh | Diễn Giải Nghiệp Vụ | Ràng Buộc Kế Nhiệm | Công Thức Tính ES Yêu Cầu ($ES_S^{req}$) |
| :--- | :--- | :--- | :--- | :--- |
| **FS** | Finish-to-Start | Kết thúc - Bắt đầu ($S$ bắt đầu sau khi $P$ hoàn thành) | $ES_S \ge EF_P + Lag$ | $ES_S^{req} = EF_P + Lag$ |
| **SS** | Start-to-Start | Bắt đầu - Bắt đầu ($S$ bắt đầu sau khi $P$ bắt đầu) | $ES_S \ge ES_P + Lag$ | $ES_S^{req} = ES_P + Lag$ |
| **FF** | Finish-to-Finish | Kết thúc - Kết thúc ($S$ kết thúc sau khi $P$ kết thúc) | $EF_S \ge EF_P + Lag$ | $ES_S^{req} = EF_P + Lag - D_S$ |
| **SF** | Start-to-Finish | Bắt đầu - Kết thúc ($S$ kết thúc sau khi $P$ bắt đầu) | $EF_S \ge ES_P + Lag$ | $ES_S^{req} = ES_P + Lag - D_S$ |

---

## 2. Thuật Toán Duyệt Tiến Forward Pass (Subtask T-19 - NTDHTCT-145)

1. **Kiểm tra đồ thị mạng (Graph Validation & Cycle Detection)**:
   - Mạng tiến độ phải là một đồ thị có hướng không chu trình (DAG - Directed Acyclic Graph).
   - Áp dụng thuật toán sắp xếp Topo (Topological Sort / Kahn's Algorithm). Nếu phát hiện chu trình vòng kín, trả về lỗi `SCHEDULE_CYCLE_DETECTED` (HTTP 400).
   - Chặn liên kết tự thân (Self-dependency) với mã lỗi `SELF_DEPENDENCY_NOT_ALLOWED`.

2. **Duyệt tiến theo thứ tự Topo (Forward Pass)**:
   - Các công việc gốc không có tiền nhiệm:
     $$ES_{root} = 0.0$$
     $$EF_{root} = ES_{root} + D_{root} = D_{root}$$
   - Các công việc có một hoặc nhiều tiền nhiệm $P \in Pred(S)$:
     $$ES_S = \max \left( 0.0, \max_{P \in Pred(S)} \{ ES_S^{req}(P) \} \right)$$
     $$EF_S = ES_S + D_S$$
   - Tổng thời lượng của dự án:
     $$ProjectDuration = \max_{T \in Tasks} \{ EF_T \}$$

---

## 3. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-144** | T-18 - Xây dựng công thức 4 loại quan hệ | `TC-CPM-01` | Kiểm thử công thức quan hệ FS với Lag = 0, Lag > 0, Lag < 0 | $ES_S = EF_P + Lag$ chính xác 100% | **Passed** |
| **NTDHTCT-144** | T-18 - Xây dựng công thức 4 loại quan hệ | `TC-CPM-02` | Kiểm thử công thức quan hệ SS với Lag = 0, Lag > 0, Lag < 0 | $ES_S = ES_P + Lag$ chính xác 100% | **Passed** |
| **NTDHTCT-144** | T-18 - Xây dựng công thức 4 loại quan hệ | `TC-CPM-03` | Kiểm thử công thức quan hệ FF với các giá trị thời lượng $D_S$ khác nhau | $EF_S \ge EF_P + Lag$, $ES_S = EF_P + Lag - D_S$ | **Passed** |
| **NTDHTCT-144** | T-18 - Xây dựng công thức 4 loại quan hệ | `TC-CPM-04` | Kiểm thử công thức quan hệ SF với mốc chuyển tiếp | $EF_S \ge ES_P + Lag$, $ES_S = ES_P + Lag - D_S$ | **Passed** |
| **NTDHTCT-144** | T-18 - Xây dựng công thức 4 loại quan hệ | `TC-CPM-05` | Kiểm tra tính hợp lệ đầu vào (Loại quan hệ null, thời lượng âm) | Ném ngoại lệ `BadRequestException` tương ứng | **Passed** |
| **NTDHTCT-145** | T-19 - Tính ES và EF | `TC-CPM-06` | Tính toán Forward Pass trên chuỗi công việc đơn giản và tuần tự (A -> B -> C) | $ES$ và $EF$ tính đúng nối tiếp, tổng thời gian chuẩn | **Passed** |
| **NTDHTCT-145** | T-19 - Tính ES và EF | `TC-CPM-07` | Quy tắc hội tụ phân nhánh (Max rule): công việc có nhiều tiền nhiệm | $ES$ lấy giá trị $\max$ của tất cả các ràng buộc tiền nhiệm | **Passed** |
| **NTDHTCT-145** | T-19 - Tính ES và EF | `TC-CPM-08` | Mạng hỗn hợp đầy đủ cả 4 loại quan hệ FS, SS, FF, SF và có độ trễ Lag | Toàn bộ các công việc thỏa mãn đồng thời các ràng buộc | **Passed** |
| **NTDHTCT-145** | T-19 - Tính ES và EF | `TC-CPM-09` | Phát hiện chu trình vòng lặp phụ thuộc (Cycle Detection) | Trả về HTTP 400 Bad Request, mã lỗi `SCHEDULE_CYCLE_DETECTED` | **Passed** |
| **NTDHTCT-145** | T-19 - Tính ES và EF | `TC-CPM-10` | Từ chối công việc tự phụ thuộc vào chính mình | Trả về HTTP 400 Bad Request, mã lỗi `SELF_DEPENDENCY_NOT_ALLOWED` | **Passed** |
| **NTDHTCT-143** | S-08 - Tích hợp API Tiến độ & RBAC | `TC-CPM-11` | Kích hoạt Forward Pass qua REST API `POST /api/projects/{projectId}/schedule/forward-pass` | Cập nhật $ES$, $EF$ vào Database, HTTP 200 OK | **Passed** |
| **NTDHTCT-143** | S-08 - Tích hợp API Tiến độ & RBAC | `TC-CPM-12` | Kiểm tra phân quyền RBAC: WORKER không có quyền tính toán tiến độ | Bị chặn bởi Middleware Authorization với HTTP 403 Forbidden | **Passed** |

---

## 4. Chi Tiết Các Kịch Bản Test Chính

### Kịch bản 1: Tạo liên kết phụ thuộc (T-18 / NTDHTCT-144)
- **Endpoint**: `POST /api/projects/{projectId}/dependencies`
- **Header**: `X-User-Id: 1` (Role: PROJECT_MANAGER)
- **Body**:
  ```json
  {
    "predecessorId": 1,
    "successorId": 2,
    "type": "FS",
    "lag": 1.0
  }
  ```
- **Kết quả**: HTTP 201 Created, quan hệ được lưu trữ với type = `FS`, lag = `1.0`.

### Kịch bản 2: Kích hoạt tính toán Forward Pass ES/EF (T-19 / NTDHTCT-145)
- **Endpoint**: `POST /api/projects/{projectId}/schedule/forward-pass?saveToDb=true`
- **Header**: `X-User-Id: 1` (Role: PROJECT_MANAGER)
- **Kết quả**: HTTP 200 OK
  ```json
  {
    "success": true,
    "message": "Tính toán thời điểm khởi sớm (ES) và kết thúc sớm (EF) thành công",
    "data": {
      "projectId": 1,
      "projectDuration": 10.0,
      "totalTasks": 3,
      "schedule": [
        {
          "taskId": 1,
          "code": "A",
          "name": "Công việc A",
          "duration": 3.0,
          "earlyStart": 0.0,
          "earlyFinish": 3.0
        },
        {
          "taskId": 2,
          "code": "B",
          "name": "Công việc B",
          "duration": 4.0,
          "earlyStart": 3.0,
          "earlyFinish": 7.0
        },
        {
          "taskId": 3,
          "code": "C",
          "name": "Công việc C",
          "duration": 2.0,
          "earlyStart": 8.0,
          "earlyFinish": 10.0
        }
      ]
    }
  }
  ```

### Kịch bản 3: Phát hiện mạng có chu trình kín (T-19 / NTDHTCT-145)
- **Endpoint**: `POST /api/projects/{projectId}/schedule/forward-pass`
- **Header**: `X-User-Id: 1` (Project có chu trình A -> B và B -> A)
- **Kết quả**: HTTP 400 Bad Request
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "code": "SCHEDULE_CYCLE_DETECTED",
    "message": "Phát hiện chu trình phụ thuộc vòng kín (Circular Dependency / Cycle) trong mạng công việc. Các công việc liên quan: [1, 2]"
  }
  ```
