package com.ntdhtcct.service.cpm;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.entity.DependencyType;

/**
 * T-18 (NTDHTCT-144): Xây dựng công thức cho bốn loại quan hệ phụ thuộc trong tiến độ thi công.
 *
 * Trong phương pháp sơ đồ quan hệ mạng PDM (Precedence Diagramming Method):
 * Giả sử công việc tiền nhiệm (Predecessor) có:
 *   - ES(P): Khởi sớm của tiền nhiệm
 *   - EF(P): Kết thúc sớm của tiền nhiệm (EF = ES + Duration)
 * Và công việc kế nhiệm (Successor) có:
 *   - D(S): Thời lượng công việc kế nhiệm (Duration >= 0)
 *   - ES(S): Khởi sớm của kế nhiệm
 *   - EF(S): Kết thúc sớm của kế nhiệm (EF = ES + Duration)
 *   - Lag: Độ trễ của mối liên kết
 *
 * Bốn công thức xác định mốc khởi sớm tối thiểu yêu cầu (ES requirement) đặt lên công việc kế nhiệm S:
 * 1. FS (Finish-to-Start):
 *    Ràng buộc: ES(S) >= EF(P) + Lag
 *    Công thức: ES_req = EF(P) + Lag
 *
 * 2. SS (Start-to-Start):
 *    Ràng buộc: ES(S) >= ES(P) + Lag
 *    Công thức: ES_req = ES(P) + Lag
 *
 * 3. FF (Finish-to-Finish):
 *    Ràng buộc: EF(S) >= EF(P) + Lag
 *    Vì EF(S) = ES(S) + D(S)  =>  ES(S) + D(S) >= EF(P) + Lag  =>  ES(S) >= EF(P) + Lag - D(S)
 *    Công thức: ES_req = EF(P) + Lag - D(S)
 *
 * 4. SF (Start-to-Finish):
 *    Ràng buộc: EF(S) >= ES(P) + Lag
 *    Vì EF(S) = ES(S) + D(S)  =>  ES(S) + D(S) >= ES(P) + Lag  =>  ES(S) >= ES(P) + Lag - D(S)
 *    Công thức: ES_req = ES(P) + Lag - D(S)
 */
public final class DependencyFormulaCalculator {

    private DependencyFormulaCalculator() {
        // Utility class
    }

    /**
     * Tính toán yêu cầu khởi sớm tối thiểu (Early Start Requirement) đặt lên công việc kế nhiệm S
     * từ công việc tiền nhiệm P thông qua loại quan hệ và độ trễ.
     *
     * @param predEs       Thời điểm khởi sớm của tiền nhiệm P (ES_P >= 0)
     * @param predEf       Thời điểm kết thúc sớm của tiền nhiệm P (EF_P >= ES_P)
     * @param succDuration Thời lượng của công việc kế nhiệm S (D_S >= 0)
     * @param type         Loại quan hệ (FS, SS, FF, SF)
     * @param lag          Độ trễ liên kết (Lag, có thể dương, âm hoặc bằng 0)
     * @return Mốc khởi sớm yêu cầu đối với S (trước khi so sánh với mốc 0 của dự án)
     */
    public static double calculateEarlyStartRequirement(
            double predEs,
            double predEf,
            double succDuration,
            DependencyType type,
            double lag) {

        if (type == null) {
            throw new BadRequestException("INVALID_DEPENDENCY_TYPE", "Loại quan hệ phụ thuộc không được để trống");
        }
        if (succDuration < 0) {
            throw new BadRequestException("INVALID_DURATION", "Thời lượng công việc kế nhiệm không được âm: " + succDuration);
        }

        return switch (type) {
            case FS -> calculateFsEarlyStart(predEf, lag);
            case SS -> calculateSsEarlyStart(predEs, lag);
            case FF -> calculateFfEarlyStart(predEf, succDuration, lag);
            case SF -> calculateSfEarlyStart(predEs, succDuration, lag);
        };
    }

    /**
     * Công thức 1: Finish-to-Start (FS)
     * ES(S) >= EF(P) + Lag
     */
    public static double calculateFsEarlyStart(double predEf, double lag) {
        return predEf + lag;
    }

    /**
     * Công thức 2: Start-to-Start (SS)
     * ES(S) >= ES(P) + Lag
     */
    public static double calculateSsEarlyStart(double predEs, double lag) {
        return predEs + lag;
    }

    /**
     * Công thức 3: Finish-to-Finish (FF)
     * EF(S) >= EF(P) + Lag  =>  ES(S) >= EF(P) + Lag - D(S)
     */
    public static double calculateFfEarlyStart(double predEf, double succDuration, double lag) {
        return predEf + lag - succDuration;
    }

    /**
     * Công thức 4: Start-to-Finish (SF)
     * EF(S) >= ES(P) + Lag  =>  ES(S) >= ES(P) + Lag - D(S)
     */
    public static double calculateSfEarlyStart(double predEs, double succDuration, double lag) {
        return predEs + lag - succDuration;
    }

    /**
     * Tính toán mốc kết thúc sớm yêu cầu tối thiểu (Early Finish Requirement) đặt lên S
     * hữu ích cho việc kiểm tra ràng buộc kết thúc (FF, SF).
     */
    public static double calculateEarlyFinishRequirement(
            double predEs,
            double predEf,
            DependencyType type,
            double lag) {

        if (type == null) {
            throw new BadRequestException("INVALID_DEPENDENCY_TYPE", "Loại quan hệ phụ thuộc không được để trống");
        }

        return switch (type) {
            case FF -> predEf + lag;
            case SF -> predEs + lag;
            default -> throw new IllegalArgumentException("Loại quan hệ " + type + " không trực tiếp ràng buộc mốc kết thúc EF");
        };
    }
}
