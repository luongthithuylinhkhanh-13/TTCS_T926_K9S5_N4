package com.ntdhtcct.entity;

/**
 * T-18 (NTDHTCT-144): Định nghĩa bốn loại quan hệ phụ thuộc giữa các công việc
 * trong phương pháp sơ đồ mạng đường găng CPM / PDM (Precedence Diagramming Method).
 *
 * Gồm:
 * 1. FS (Finish-to-Start): Kết thúc - Bắt đầu. Công việc kế nhiệm bắt đầu sau khi tiền nhiệm kết thúc.
 * 2. SS (Start-to-Start): Bắt đầu - Bắt đầu. Công việc kế nhiệm bắt đầu sau khi tiền nhiệm bắt đầu.
 * 3. FF (Finish-to-Finish): Kết thúc - Kết thúc. Công việc kế nhiệm kết thúc sau khi tiền nhiệm kết thúc.
 * 4. SF (Start-to-Finish): Bắt đầu - Kết thúc. Công việc kế nhiệm kết thúc sau khi tiền nhiệm bắt đầu.
 */
public enum DependencyType {
    /**
     * Finish-to-Start (FS): Công việc sau (Successor) chỉ có thể bắt đầu sau khi công việc trước (Predecessor) kết thúc.
     * Ràng buộc: ES(S) >= EF(P) + Lag
     */
    FS("Finish-to-Start", "Kết thúc - Bắt đầu"),

    /**
     * Start-to-Start (SS): Công việc sau (Successor) chỉ có thể bắt đầu sau khi công việc trước (Predecessor) bắt đầu.
     * Ràng buộc: ES(S) >= ES(P) + Lag
     */
    SS("Start-to-Start", "Bắt đầu - Bắt đầu"),

    /**
     * Finish-to-Finish (FF): Công việc sau (Successor) chỉ có thể hoàn thành sau khi công việc trước (Predecessor) hoàn thành.
     * Ràng buộc: EF(S) >= EF(P) + Lag <=> ES(S) >= EF(P) + Lag - Duration(S)
     */
    FF("Finish-to-Finish", "Kết thúc - Kết thúc"),

    /**
     * Start-to-Finish (SF): Công việc sau (Successor) chỉ có thể hoàn thành sau khi công việc trước (Predecessor) bắt đầu.
     * Ràng buộc: EF(S) >= ES(P) + Lag <=> ES(S) >= ES(P) + Lag - Duration(S)
     */
    SF("Start-to-Finish", "Bắt đầu - Kết thúc");

    private final String englishName;
    private final String vietnameseName;

    DependencyType(String englishName, String vietnameseName) {
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }
}
