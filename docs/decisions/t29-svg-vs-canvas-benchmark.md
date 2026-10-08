# ADR: Quyết định chọn Renderer SVG vs HTML Canvas cho Gantt Chart (T-29)

- **Trạng thái:** Đã quyết định (Approved)
- **Ngày thực hiện:** 2026-10-07
- **Phạm vi:** Spike kỹ thuật T-29 (Độc lập với production Gantt)
- **Tác giả:** Engineering Team

---

## 1. Ngữ cảnh (Context)

Hệ thống quản lý thi công công trình cần hiển thị tiến độ dự án dưới dạng biểu đồ Gantt. Trong các dự án quy mô vừa, số lượng công việc (tasks) thường ở mức khoảng 500 tasks.

Hiện tại, production Gantt trong `ProgressPage.jsx` đang sử dụng HTML `<div>` + CSS positioning. Spike T-29 được tiến hành nhằm trả lời câu hỏi kỹ thuật độc lập:
> **"Với một biểu đồ Gantt khoảng 500 tasks, renderer nào phù hợp hơn giữa SVG (`<rect>`) và HTML5 Canvas 2D?"**

Mục tiêu của spike:
- Đo lường hiệu năng thực tế trên cùng một bộ dataset (500 tasks deterministic) và cùng layout calculation.
- Đánh giá trên cả hai môi trường: Desktop và Mobile Emulation (4x CPU slowdown).
- Dựa trên số liệu đo đạc thực tế (không giả định) kết hợp các yếu tố công nghệ (accessibility, tương tác, tính dễ bảo trì) để đưa ra quyết định kỹ thuật.
- **Lưu ý:** Spike này độc lập và không tự ý thay đổi implementation của production Gantt hiện tại.

---

## 2. Thiết lập Benchmark (Benchmark Setup)

### 2.1. Cấu hình phần cứng và môi trường đo
- **Hệ điều hành:** Windows 11 Pro 64-bit
- **CPU:** 12th Gen Intel(R) Core(TM) i5-12450HX (8 cores, 12 threads)
- **Browser:** Google Chrome 154.0.8037.98 qua Chrome DevTools Protocol (CDP)
- **Môi trường chạy:**
  1. **Desktop:** Viewport tiêu chuẩn, CPU không giới hạn.
  2. **Mobile (Emulated):** Viewport iPhone/Android 390x844, Device Scale Factor = 3, CPU Throttling Rate = **4x slowdown**.

### 2.2. Dữ liệu & Quy cách đo
- **Dataset:** 500 tasks deterministic sinh ra từ hàm `generateBenchmarkTasks`.
- **Layout:**
  - `TOTAL_TASKS = 500`
  - `ROW_HEIGHT = 24px`
  - `BAR_HEIGHT = 16px`
  - `SCALE = 4px/time_unit`
  - Kích thước đồ thị: `width = 1000px`, `height = 12,000px`.
- **Phương pháp đo:**
  - Initial Render: 3 warm-up runs (bỏ qua) + 10 measurement runs (ghi nhận min, median, average, max). Đo thời gian giữa unmount và hoàn tất paint bằng 2x `requestAnimationFrame`.
  - Scroll Smoothness: Cuộn tự động liên tục qua 12,000px trong 3000ms, ghi nhận FPS trung bình và số lượng jank frames (frame delta > 33.33ms, tức dưới 30 FPS).
  - Interaction Re-render: Đo thời gian re-render khi cập nhật trạng thái highlight bar #250 (5 measurement runs).
  - DOM Node Overhead: Đo số phần tử DOM được tạo (`svg rect` vs `canvas`).
  - JS Heap Memory: Đo qua `performance.memory.usedJSHeapSize` (best-effort trong Chrome).

---

## 3. Bộ số liệu thực tế đo đạc (Final Measured Results)

Dữ liệu được trích xuất trực tiếp từ file kết quả chạy thực nghiệm `benchmark-final-results.json`.

### 3.1. Desktop Benchmark Results

| Tiêu chí / Chỉ số | SVG (`<rect>`) | Canvas 2D | So sánh / Nhận xét |
| :--- | :---: | :---: | :--- |
| **Initial Render - Median** | **33.20 ms** | **41.75 ms** | Chênh lệch ~1.26x (cả hai đều dưới 50ms) |
| Initial Render - Min / Avg / Max | 31.30 / 33.17 / 34.40 ms | 39.90 / 45.03 / 72.70 ms | SVG ổn định, Canvas có 1 lượt đạt 72.70 ms |
| **Raw 10 runs (ms)** | `[33.6, 31.3, 33.2, 34.4, 33.1, 34.2, 32.7, 33.2, 33.6, 32.4]` | `[72.7, 42.5, 43.6, 41.6, 40.6, 45.8, 41.9, 40.9, 39.9, 40.8]` | 10 lượt đo sau 3 warm-up runs |
| **Scroll Smoothness (FPS)** | **60.0 FPS** | **60.0 FPS** | Cả hai cuộn đạt mức tối đa 60 FPS |
| **Jank Frames (>33.3ms)** | **0 / 180 frames** | **0 / 180 frames** | 0 jank frame trong 3s cuộn |
| **Interaction Re-render (Median)** | **33.00 ms** | **33.10 ms** | Gần như tương đương hoàn toàn |
| Interaction - Min / Avg / Max | 32.90 / 33.22 / 33.70 ms | 32.40 / 33.28 / 34.00 ms | Toggle highlight bar #250 |
| Interaction - Raw 5 runs (ms) | `[33.5, 33.7, 33.0, 33.0, 32.9]` | `[33.9, 33.1, 33.0, 34.0, 32.4]` | 5 lượt đo tương tác |
| **DOM Node Count** | 500 nodes | 1 node | SVG tạo 500 thẻ `<rect>`, Canvas tạo 1 thẻ |
| **JS Heap Memory** | 8.61 MB | 4.76 MB | Canvas sử dụng ít heap JS hơn |

---

### 3.2. Mobile Benchmark Results (Emulation: 390x844, 4x CPU Throttling)

| Tiêu chí / Chỉ số | SVG (`<rect>`) | Canvas 2D | So sánh / Nhận xét |
| :--- | :---: | :---: | :--- |
| **Initial Render - Median** | **53.85 ms** | **160.25 ms** | SVG nhanh hơn trong điều kiện CPU throttle |
| Initial Render - Min / Avg / Max | 44.20 / 53.85 / 66.40 ms | 87.00 / 141.27 / 195.70 ms | Canvas dao động từ 87.0ms đến 195.7ms |
| **Raw 10 runs (ms)** | `[66.4, 48.0, 44.2, 60.5, 55.3, 55.3, 47.9, 51.5, 52.4, 57.0]` | `[195.7, 88.9, 91.4, 87.0, 168.7, 163.2, 170.0, 161.3, 159.2, 127.3]` | 10 lượt đo sau 3 warm-up runs |
| **Scroll Smoothness (FPS)** | **60.0 FPS** | **59.0 FPS** | Cả hai duy trì xấp xỉ 60 FPS |
| **Jank Frames (>33.3ms)** | **0 / 180 frames** | **1 / 177 frames** | Canvas ghi nhận 1 frame bị jank |
| **Interaction Re-render (Median)** | **44.20 ms** | **81.50 ms** | SVG phản hồi nhanh hơn khi đổi state |
| Interaction - Min / Avg / Max | 39.90 / 44.92 / 53.90 ms | 77.60 / 82.10 / 88.60 ms | Toggle highlight bar #250 |
| Interaction - Raw 5 runs (ms) | `[53.9, 44.2, 41.7, 39.9, 44.9]` | `[81.5, 88.6, 84.1, 77.6, 78.7]` | 5 lượt đo tương tác |
| **DOM Node Count** | 500 nodes | 1 node | 500 DOM nodes trên mobile |
| **JS Heap Memory** | 11.34 MB | 5.21 MB | Chênh lệch heap khoảng ~6.1 MB |

---

## 4. Nhận xét & Quan sát kỹ thuật (Technical Observations)

1. **Hiệu năng trên Desktop:**
   - Benchmark cho thấy trên máy tính để bàn (CPU đầy đủ), thời gian render lần đầu của cả hai bên đều ở mức chấp nhận được: SVG (33.20 ms) và Canvas (41.75 ms). Sự chênh lệch là ~1.26x (dưới ngưỡng 2x).
   - Tốc độ cuộn của cả hai đều đạt 60.0 FPS và không có jank frame nào.
   - Thời gian tương tác (highlight 1 thanh) trên desktop thực tế gần như tương đương nhau (~33.0 ms vs ~33.1 ms).

2. **Hiệu năng trên Mobile (4x CPU Throttle):**
   - Benchmark cho thấy khi có CPU throttling 4x, thời gian render ban đầu của SVG ổn định hơn (median 53.85 ms so với 160.25 ms của Canvas).
   - Thời gian re-render khi tương tác trên mobile: SVG đạt median 44.20 ms, trong khi Canvas đạt median 81.50 ms.
   - Một khả năng là việc vẽ lại toàn bộ kích thước bề mặt canvas 12,000px tiêu tốn chu kỳ xử lý đồ họa khi tài nguyên CPU bị hạn chế, nhưng điều này chưa được profiling chuyên sâu ở tầng GPU của trình duyệt để khẳng định tuyệt đối.
   - Tuy nhiên, số liệu thực nghiệm rõ ràng cho thấy trong môi trường mô phỏng thiết bị di động, SVG không gặp hiện tượng suy giảm hiệu năng như lo ngại đối với 500 phần tử DOM.

---

## 5. Đánh giá Trade-offs kỹ thuật (Engineering Trade-offs)

| Khía cạnh | SVG (`<rect>`) | Canvas 2D |
| :--- | :--- | :--- |
| **Hiệu năng ở quy mô 500 bars** | **Đạt yêu cầu:** 33.20 ms (Desktop), 53.85 ms (Mobile throttling). | **Chấp nhận được trên Desktop (41.75 ms);** chậm hơn khi CPU bị throttle (160.25 ms). |
| **Tương tác & Event Handling** | **Rất thuận tiện:** Tận dụng trực tiếp hệ thống React synthetic events (`onClick`, `onMouseEnter`) trên từng phần tử `<rect>`. | **Phức tạp:** Phải tự lập trình logic hit-testing dựa trên tọa độ chuột $(x, y)$ và bounding box của từng task. |
| **Khả năng tiếp cận (Accessibility - a11y)** | **Hỗ trợ tự nhiên:** Dễ dàng gán `aria-label`, `role="img"`, hỗ trợ định vị focus cho screen reader. | **Hạn chế:** Bản thân thẻ canvas là một vùng bitmap, đòi hỏi phải xây dựng thêm lớp fallback DOM nếu muốn hỗ trợ a11y. |
| **Tích hợp giao diện & CSS** | **Linh hoạt:** Dễ kết hợp với Tooltip (Ant Design), CSS hover states, styling theo trạng thái. | **Khó hơn:** Toàn bộ hiệu ứng hover, highlight cần được điều khiển bằng code imperative trong canvas loop. |
| **Độ phức tạp mã nguồn** | **Thấp:** Cú pháp JSX mang tính declarative, dễ đọc và dễ bảo trì cho cả nhóm. | **Trung bình:** Cần quản lý vòng đời canvas ref, context 2D, scale device pixel ratio. |
| **Quy mô lớn (>3,000 tasks)** | Sẽ gặp áp lực về số lượng DOM nodes nếu không áp dụng kỹ thuật windowing/virtualization. | Có lợi thế nếu số lượng thanh lên tới hàng chục nghìn mà không cần DOM nodes. |

---

## 6. Quyết định kỹ thuật (Final Recommendation & Decision)

**Quyết định: Chọn SVG cho biểu đồ Gantt ở quy mô ~500 tasks.**

### Cơ sở quyết định (Dựa trên Framework của Plan):
1. **Dữ liệu thực nghiệm:**
   - Trên desktop, cả hai renderer đều nằm trong ngưỡng hiệu năng tốt (render dưới 45ms, scroll 60 FPS, 0 jank).
   - Trên mobile emulated (CPU throttle 4x), SVG duy trì thời gian render và tương tác tốt hơn so với Canvas trong phép đo này.
2. **Kiến trúc và trải nghiệm phát triển:**
   - Với quy mô 500 tasks, chi phí 500 DOM elements là hoàn toàn nằm trong vùng an toàn của trình duyệt hiện đại.
   - SVG mang lại lợi thế vượt trội về việc gắn event handlers (click, hover để xem CPM parameters), khả năng tiếp cận (accessibility), và việc tích hợp tự nhiên vào hệ sinh thái React của dự án mà không cần cài đặt thêm thư viện tính toán hit-test.

### Phạm vi và Giới hạn (Scope & Limitations):
- Quyết định này áp dụng cho biểu đồ Gantt ở quy mô **khoảng 500 tasks**.
- Nếu trong tương lai hệ thống cần mở rộng lên **trên 3,000 tasks**, giải pháp khuyến nghị là áp dụng kỹ thuật **Virtualization (Windowing)** cho SVG (chỉ render các task nằm trong khung nhìn viewport).
- **Quyết định này là tài liệu kiến trúc; không tự động sửa đổi hoặc refactor implementation của production Gantt hiện tại (`ProgressPage.jsx`).**
