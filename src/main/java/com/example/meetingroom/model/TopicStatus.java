package com.example.meetingroom.model;

public enum TopicStatus { // Các trạng thái trong vòng đời đề tài.
    DRAFT("Bản nháp"), // Giáo viên đã lưu nhưng chưa công bố.
    APPROVED("Đã duyệt"),
    PUBLISHED("Đã công bố"), // Sinh viên có thể đăng ký.
    ASSIGNED("Đã phân công"), // Nhóm đã được duyệt nhận đề tài.
    COMPLETED("Đã hoàn thành"); // Đã chấm và công bố kết quả.

    private final String label; // Nhãn trạng thái trên giao diện.

    TopicStatus(String label) {
        this.label = label;
    } // Lưu nhãn cho trạng thái.

    public String getLabel() {
        return label;
    } // Trả về nhãn hiển thị.
}