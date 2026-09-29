package com.ntdhtcct;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.entity.DependencyType;
import com.ntdhtcct.service.cpm.DependencyFormulaCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-18 (NTDHTCT-144): Unit Test cho công thức của bốn loại quan hệ phụ thuộc:
 * 1. FS (Finish-to-Start): ES(S) >= EF(P) + Lag
 * 2. SS (Start-to-Start): ES(S) >= ES(P) + Lag
 * 3. FF (Finish-to-Finish): EF(S) >= EF(P) + Lag <=> ES(S) >= EF(P) + Lag - D(S)
 * 4. SF (Start-to-Finish): EF(S) >= ES(P) + Lag <=> ES(S) >= ES(P) + Lag - D(S)
 */
public class DependencyFormulaCalculatorTest {

    @Nested
    @DisplayName("1. Quan hệ FS (Finish-to-Start)")
    class FinishToStartTests {

        @Test
        @DisplayName("FS với Lag = 0: ES(S) = EF(P)")
        void testFsWithZeroLag() {
            double predEs = 0.0;
            double predEf = 10.0;
            double succDuration = 5.0;
            double lag = 0.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FS, lag);

            assertEquals(10.0, esReq);
        }

        @Test
        @DisplayName("FS với Lag > 0: ES(S) = EF(P) + Lag")
        void testFsWithPositiveLag() {
            double predEs = 2.0;
            double predEf = 7.0;
            double succDuration = 4.0;
            double lag = 3.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FS, lag);

            assertEquals(10.0, esReq);
        }

        @Test
        @DisplayName("FS với Lag < 0 (Lead time / Bắt đầu gối đầu sớm): ES(S) = EF(P) - |Lag|")
        void testFsWithNegativeLag() {
            double predEs = 0.0;
            double predEf = 8.0;
            double succDuration = 3.0;
            double lag = -2.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FS, lag);

            assertEquals(6.0, esReq);
        }
    }

    @Nested
    @DisplayName("2. Quan hệ SS (Start-to-Start)")
    class StartToStartTests {

        @Test
        @DisplayName("SS với Lag = 0: ES(S) = ES(P)")
        void testSsWithZeroLag() {
            double predEs = 4.0;
            double predEf = 12.0;
            double succDuration = 6.0;
            double lag = 0.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.SS, lag);

            assertEquals(4.0, esReq);
        }

        @Test
        @DisplayName("SS với Lag > 0: ES(S) = ES(P) + Lag")
        void testSsWithPositiveLag() {
            double predEs = 3.0;
            double predEf = 9.0;
            double succDuration = 5.0;
            double lag = 2.5;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.SS, lag);

            assertEquals(5.5, esReq);
        }

        @Test
        @DisplayName("SS với Lag < 0: ES(S) = ES(P) - |Lag|")
        void testSsWithNegativeLag() {
            double predEs = 5.0;
            double predEf = 15.0;
            double succDuration = 4.0;
            double lag = -2.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.SS, lag);

            assertEquals(3.0, esReq);
        }
    }

    @Nested
    @DisplayName("3. Quan hệ FF (Finish-to-Finish)")
    class FinishToFinishTests {

        @Test
        @DisplayName("FF với Lag = 0: EF(S) >= EF(P) => ES(S) = EF(P) - D(S)")
        void testFfWithZeroLag() {
            double predEs = 2.0;
            double predEf = 10.0;
            double succDuration = 4.0;
            double lag = 0.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FF, lag);

            // ES = 10.0 + 0 - 4.0 = 6.0 (khi đó EF(S) = 6 + 4 = 10.0 >= EF(P))
            assertEquals(6.0, esReq);
            assertEquals(10.0, DependencyFormulaCalculator.calculateEarlyFinishRequirement(predEs, predEf, DependencyType.FF, lag));
        }

        @Test
        @DisplayName("FF với Lag > 0: ES(S) = EF(P) + Lag - D(S)")
        void testFfWithPositiveLag() {
            double predEs = 0.0;
            double predEf = 8.0;
            double succDuration = 5.0;
            double lag = 2.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FF, lag);

            // ES = 8 + 2 - 5 = 5.0 (khi đó EF(S) = 5 + 5 = 10.0 >= 8 + 2)
            assertEquals(5.0, esReq);
        }

        @Test
        @DisplayName("FF khi D(S) > EF(P) + Lag: ES tính theo công thức trả về số âm")
        void testFfWhenDurationExceedsFinish() {
            double predEs = 0.0;
            double predEf = 6.0;
            double succDuration = 8.0;
            double lag = 0.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.FF, lag);

            // 6 - 8 = -2 (thuật toán duyệt tiến sẽ max(0, -2) = 0 khi áp dụng vào mạng)
            assertEquals(-2.0, esReq);
        }
    }

    @Nested
    @DisplayName("4. Quan hệ SF (Start-to-Finish)")
    class StartToFinishTests {

        @Test
        @DisplayName("SF với Lag = 0: EF(S) >= ES(P) => ES(S) = ES(P) - D(S)")
        void testSfWithZeroLag() {
            double predEs = 7.0;
            double predEf = 15.0;
            double succDuration = 3.0;
            double lag = 0.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.SF, lag);

            // ES = 7 + 0 - 3 = 4.0 (khi đó EF(S) = 4 + 3 = 7.0 >= ES(P))
            assertEquals(4.0, esReq);
            assertEquals(7.0, DependencyFormulaCalculator.calculateEarlyFinishRequirement(predEs, predEf, DependencyType.SF, lag));
        }

        @Test
        @DisplayName("SF với Lag > 0: ES(S) = ES(P) + Lag - D(S)")
        void testSfWithPositiveLag() {
            double predEs = 5.0;
            double predEf = 10.0;
            double succDuration = 4.0;
            double lag = 3.0;

            double esReq = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                    predEs, predEf, succDuration, DependencyType.SF, lag);

            // ES = 5 + 3 - 4 = 4.0 (khi đó EF(S) = 4 + 4 = 8.0 >= 5 + 3)
            assertEquals(4.0, esReq);
        }
    }

    @Nested
    @DisplayName("5. Kiểm tra tính hợp lệ dữ liệu (Validation)")
    class ValidationTests {

        @Test
        @DisplayName("Ném ngoại lệ nếu loại quan hệ bị null")
        void testNullDependencyType() {
            assertThrows(BadRequestException.class, () ->
                    DependencyFormulaCalculator.calculateEarlyStartRequirement(0, 5, 2, null, 0));
        }

        @Test
        @DisplayName("Ném ngoại lệ nếu thời lượng công việc kế nhiệm bị âm")
        void testNegativeDuration() {
            assertThrows(BadRequestException.class, () ->
                    DependencyFormulaCalculator.calculateEarlyStartRequirement(0, 5, -1.0, DependencyType.FS, 0));
        }
    }
}
