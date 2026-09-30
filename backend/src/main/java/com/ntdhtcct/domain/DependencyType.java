package com.ntdhtcct.domain;

public enum DependencyType {
    FS("Finish-to-Start", "Kết thúc - Bắt đầu"),
    SS("Start-to-Start", "Bắt đầu - Bắt đầu"),
    FF("Finish-to-Finish", "Kết thúc - Kết thúc"),
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