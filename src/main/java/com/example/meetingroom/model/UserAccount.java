package com.example.meetingroom.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UserAccount { // Dữ liệu tài khoản và vai trò trong phiên đăng nhập.
    private String username; // Tên đăng nhập; sinh viên dùng MSSV.
    private String password; // Mật khẩu đã mã hóa BCrypt.
    private String displayName; // Tên hiển thị trên dashboard.
    private String studentId; // MSSV dùng để lọc dữ liệu sinh viên.
    private UserRole role; // Vai trò quyết định quyền thao tác.

    public UserAccount() {
    } // Constructor rỗng dành cho Jackson khi nạp tài khoản.

    public UserAccount(String username, String password, String displayName, String studentId, UserRole role) { // Tạo
                                                                                                                // tài
                                                                                                                // khoản
                                                                                                                // mẫu.
        this.username = username; // Gán tên đăng nhập.
        this.password = password; // Gán mật khẩu.
        this.displayName = displayName; // Gán tên hiển thị.
        this.studentId = studentId; // Gán MSSV hoặc null với giáo viên.
        this.role = role; // Gán quyền người dùng.
    }

    public String getUsername() {
        return username;
    } // Cung cấp username để xác thực.

    public String getDisplayName() {
        return displayName;
    } // Cung cấp tên hiển thị.

    public String getStudentId() {
        return studentId;
    } // Cung cấp MSSV để lọc nhóm.

    public UserRole getRole() {
        return role;
    } // Cung cấp vai trò cho controller.

    public String getPasswordHash() {
        return password;
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public boolean isTeacher() {
        return role == UserRole.ADMIN || role == UserRole.DEPARTMENT_HEAD
                || role == UserRole.TEACHER || role == UserRole.SUPERVISOR
                || role == UserRole.REVIEWER || role == UserRole.COUNCIL;
    } // Kiểm tra tài khoản giáo viên.

    // Vai trò khoa được xem các màn hình quản lý nhưng quyền nhạy cảm vẫn kiểm tra riêng bằng isAdmin().
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    } // Kiểm tra quyền ban chủ nhiệm khoa.

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    } // Kiểm tra tài khoản sinh viên.

    public boolean canSupervise() {
        return role == UserRole.TEACHER || role == UserRole.SUPERVISOR;
    }
}