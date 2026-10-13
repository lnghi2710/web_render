package com.example.meetingroom.model;

public enum UserRole { // Vai trò dùng để giới hạn quyền thao tác.
    ADMIN("Trưởng khoa"),
    DEPARTMENT_HEAD("Trưởng bộ môn"),
    TEACHER("Giảng viên"),
    SUPERVISOR("Giảng viên hướng dẫn"),
    REVIEWER("Giảng viên phản biện"),
    COUNCIL("Hội đồng"),
    STUDENT("Sinh viên");

    private final String label; // Tên vai trò hiển thị.

    UserRole(String label) {
        this.label = label;
    } // Lưu nhãn vai trò.

    public String getLabel() {
        return label;
    } // Cung cấp nhãn cho giao diện.
}