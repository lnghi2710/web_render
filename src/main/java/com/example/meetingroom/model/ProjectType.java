package com.example.meetingroom.model;

public enum ProjectType { // Các loại đợt đăng ký được hệ thống hỗ trợ.
    MON_HOC("Môn học"), // Đợt đề tài môn học.
    NCKH("NCKH"), // Đợt nghiên cứu khoa học.
    TLCN("TLCN"), // Đợt tiểu luận chuyên ngành.
    KLTN("KLTN"); // Đợt khóa luận tốt nghiệp.

    private final String label; // Tên tiếng Việt hiển thị trên giao diện.

    ProjectType(String label) { // Gắn nhãn hiển thị cho từng loại.
        this.label = label; // Lưu nhãn của loại đề tài.
    }

    public String getLabel() { // Cung cấp nhãn cho Thymeleaf.
        return label; // Trả về nhãn đã lưu.
    }
}