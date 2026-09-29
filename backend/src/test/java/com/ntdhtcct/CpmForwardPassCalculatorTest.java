package com.ntdhtcct;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.entity.DependencyType;
import com.ntdhtcct.service.cpm.CpmForwardPassCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-19 (NTDHTCT-145): Unit Test cho thuật toán duyệt tiến (Forward Pass Calculation)
 * tính thời điểm khởi sớm (ES) và kết thúc sớm (EF) cho toàn bộ mạng công việc dự án.
 */
public class CpmForwardPassCalculatorTest {

    private CpmForwardPassCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CpmForwardPassCalculator();
    }

    @Nested
    @DisplayName("1. Mạng công việc cơ bản")
    class BasicNetworkTests {

        @Test
        @DisplayName("Danh sách công việc rỗng: thời lượng dự án bằng 0")
        void testEmptyTaskList() {
            var output = calculator.calculate(List.of(), List.of());
            assertEquals(0.0, output.projectDuration());
            assertTrue(output.taskResults().isEmpty());
        }

        @Test
        @DisplayName("Một công việc duy nhất: ES = 0, EF = Duration")
        void testSingleTask() {
            var tasks = List.of(new CpmForwardPassCalculator.TaskInput(1L, "A", "Công việc A", 5.0));
            var output = calculator.calculate(tasks, List.of());

            assertEquals(5.0, output.projectDuration());
            var resA = output.taskResults().get(1L);
            assertEquals(0.0, resA.earlyStart());
            assertEquals(5.0, resA.earlyFinish());
        }

        @Test
        @DisplayName("Chuỗi tuần tự nối tiếp đơn giản A -> B -> C (FS, Lag = 0)")
        void testLinearSequence() {
            // A(dur=3) -> B(dur=4) -> C(dur=2)
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Đào móng", 3.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Đổ bê tông móng", 4.0),
                    new CpmForwardPassCalculator.TaskInput(3L, "C", "Bảo dưỡng bê tông", 2.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(101L, 1L, 2L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(102L, 2L, 3L, DependencyType.FS, 0.0)
            );

            var output = calculator.calculate(tasks, deps);

            // A: ES = 0, EF = 3
            var resA = output.taskResults().get(1L);
            assertEquals(0.0, resA.earlyStart());
            assertEquals(3.0, resA.earlyFinish());

            // B: ES = 3, EF = 7
            var resB = output.taskResults().get(2L);
            assertEquals(3.0, resB.earlyStart());
            assertEquals(7.0, resB.earlyFinish());

            // C: ES = 7, EF = 9
            var resC = output.taskResults().get(3L);
            assertEquals(7.0, resC.earlyStart());
            assertEquals(9.0, resC.earlyFinish());

            // Tổng thời lượng dự án = 9.0
            assertEquals(9.0, output.projectDuration());
        }

        @Test
        @DisplayName("Chuỗi tuần tự với Lag: A -> B (FS, Lag = 2)")
        void testLinearSequenceWithLag() {
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Đổ bê tông", 3.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Tháo ván khuôn", 2.0)
            );
            // B cần chờ A xong 2 ngày dưỡng hộ
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(101L, 1L, 2L, DependencyType.FS, 2.0)
            );

            var output = calculator.calculate(tasks, deps);

            var resA = output.taskResults().get(1L);
            assertEquals(0.0, resA.earlyStart());
            assertEquals(3.0, resA.earlyFinish());

            var resB = output.taskResults().get(2L);
            // ES(B) = EF(A) + Lag = 3 + 2 = 5
            assertEquals(5.0, resB.earlyStart());
            // EF(B) = 5 + 2 = 7
            assertEquals(7.0, resB.earlyFinish());

            assertEquals(7.0, output.projectDuration());
        }
    }

    @Nested
    @DisplayName("2. Mạng phân nhánh và hội tụ (Quy tắc Max)")
    class BranchingAndConvergenceTests {

        @Test
        @DisplayName("Hội tụ nhiều tiền nhiệm: ES(D) = max(EF(B), EF(C))")
        void testConvergenceMaxRule() {
            // A -> B(dur=4) -> D
            // A -> C(dur=7) -> D
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Chuẩn bị", 2.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Gia công thép", 4.0),
                    new CpmForwardPassCalculator.TaskInput(3L, "C", "Lắp đặt cốt pha", 7.0),
                    new CpmForwardPassCalculator.TaskInput(4L, "D", "Nghiệm thu", 1.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(1L, 1L, 2L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(2L, 1L, 3L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(3L, 2L, 4L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(4L, 3L, 4L, DependencyType.FS, 0.0)
            );

            var output = calculator.calculate(tasks, deps);

            // A: ES=0, EF=2
            assertEquals(0.0, output.taskResults().get(1L).earlyStart());
            assertEquals(2.0, output.taskResults().get(1L).earlyFinish());

            // B: ES=2, EF=6
            assertEquals(2.0, output.taskResults().get(2L).earlyStart());
            assertEquals(6.0, output.taskResults().get(2L).earlyFinish());

            // C: ES=2, EF=9
            assertEquals(2.0, output.taskResults().get(3L).earlyStart());
            assertEquals(9.0, output.taskResults().get(3L).earlyFinish());

            // D: ES = max(EF(B), EF(C)) = max(6, 9) = 9, EF = 9 + 1 = 10
            var resD = output.taskResults().get(4L);
            assertEquals(9.0, resD.earlyStart());
            assertEquals(10.0, resD.earlyFinish());

            assertEquals(10.0, output.projectDuration());
        }
    }

    @Nested
    @DisplayName("3. Mạng hỗn hợp 4 loại quan hệ (FS, SS, FF, SF)")
    class MixedDependencyTypesTests {

        @Test
        @DisplayName("Mạng chứa đồng thời quan hệ FS, SS, FF và độ trễ Lag")
        void testMixedDependenciesNetwork() {
            // Task A: dur = 6 (Root)
            // Task B: dur = 4, quan hệ SS với A (Lag = 2)
            // Task C: dur = 5, quan hệ FF với A (Lag = 3)
            // Task D: dur = 3, quan hệ FS với B (Lag = 0) VÀ FF với C (Lag = 1)
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Thi công móng", 6.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Lắp đặt ống ngầm", 4.0),
                    new CpmForwardPassCalculator.TaskInput(3L, "C", "Xây tường tầng 1", 5.0),
                    new CpmForwardPassCalculator.TaskInput(4L, "D", "Hoàn thiện tầng 1", 3.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(1L, 1L, 2L, DependencyType.SS, 2.0),
                    new CpmForwardPassCalculator.DependencyInput(2L, 1L, 3L, DependencyType.FF, 3.0),
                    new CpmForwardPassCalculator.DependencyInput(3L, 2L, 4L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(4L, 3L, 4L, DependencyType.FF, 1.0)
            );

            var output = calculator.calculate(tasks, deps);

            // A: ES=0, EF=6
            var resA = output.taskResults().get(1L);
            assertEquals(0.0, resA.earlyStart());
            assertEquals(6.0, resA.earlyFinish());

            // B (SS với A, lag=2): ES = ES(A) + 2 = 0 + 2 = 2. EF = 2 + 4 = 6
            var resB = output.taskResults().get(2L);
            assertEquals(2.0, resB.earlyStart());
            assertEquals(6.0, resB.earlyFinish());

            // C (FF với A, lag=3): EF(C) >= EF(A) + 3 = 6 + 3 = 9 => ES(C) >= 9 - 5 = 4.
            // ES = max(0, 4) = 4, EF = 4 + 5 = 9
            var resC = output.taskResults().get(3L);
            assertEquals(4.0, resC.earlyStart());
            assertEquals(9.0, resC.earlyFinish());

            // D có 2 ràng buộc:
            // 1) FS từ B (lag=0): ES(D) >= EF(B) + 0 = 6
            // 2) FF từ C (lag=1): EF(D) >= EF(C) + 1 = 9 + 1 = 10 => ES(D) >= 10 - 3 = 7
            // => ES(D) = max(6, 7) = 7. EF(D) = 7 + 3 = 10
            var resD = output.taskResults().get(4L);
            assertEquals(7.0, resD.earlyStart());
            assertEquals(10.0, resD.earlyFinish());

            assertEquals(10.0, output.projectDuration());
        }

        @Test
        @DisplayName("Kiểm tra quan hệ SF (Start-to-Finish)")
        void testStartToFinishDependency() {
            // Task A: dur = 5 (Root) -> ES=0, EF=5
            // Task B: dur = 3, quan hệ SF với A (Lag = 7):
            // EF(B) >= ES(A) + Lag = 0 + 7 = 7 => ES(B) >= 7 - 3 = 4
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Hệ thống cũ ngừng", 5.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Vận hành thử nghiệm hệ thống mới", 3.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(1L, 1L, 2L, DependencyType.SF, 7.0)
            );

            var output = calculator.calculate(tasks, deps);

            var resB = output.taskResults().get(2L);
            assertEquals(4.0, resB.earlyStart());
            assertEquals(7.0, resB.earlyFinish());
        }
    }

    @Nested
    @DisplayName("4. Phát hiện chu trình (Cycle Detection)")
    class CycleDetectionTests {

        @Test
        @DisplayName("Phát hiện chu trình vòng kín A -> B -> C -> A và ném ngoại lệ SCHEDULE_CYCLE_DETECTED")
        void testCycleDetectionThrowsException() {
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Công việc A", 2.0),
                    new CpmForwardPassCalculator.TaskInput(2L, "B", "Công việc B", 3.0),
                    new CpmForwardPassCalculator.TaskInput(3L, "C", "Công việc C", 4.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(1L, 1L, 2L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(2L, 2L, 3L, DependencyType.FS, 0.0),
                    new CpmForwardPassCalculator.DependencyInput(3L, 3L, 1L, DependencyType.FS, 0.0) // Tạo chu trình!
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    calculator.calculate(tasks, deps));

            assertEquals("SCHEDULE_CYCLE_DETECTED", ex.getErrorCode());
        }

        @Test
        @DisplayName("Từ chối công việc tự phụ thuộc vào chính mình (Self-dependency)")
        void testSelfDependencyThrowsException() {
            var tasks = List.of(
                    new CpmForwardPassCalculator.TaskInput(1L, "A", "Công việc A", 2.0)
            );
            var deps = List.of(
                    new CpmForwardPassCalculator.DependencyInput(1L, 1L, 1L, DependencyType.FS, 0.0)
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    calculator.calculate(tasks, deps));

            assertEquals("SELF_DEPENDENCY_NOT_ALLOWED", ex.getErrorCode());
        }
    }
}
