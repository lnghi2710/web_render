package com.example.meetingroom.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

// Cho phép Jackson khôi phục trực tiếp trạng thái nội bộ từ tệp lưu, kể cả các thuộc tính không có setter.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StudentTopic { // Đề tài được giáo viên đề xuất cho một đợt.
    private Long id; // Mã đề tài.
    private String title; // Tên đề tài.
    private String description; // Nội dung và mục tiêu đề tài.
    private String department; // Bộ môn quản lý đề tài.
    private String supervisor; // Giáo viên hướng dẫn chính.
    private String coSupervisor; // Giáo viên hướng dẫn thứ hai nếu có.
    private TopicStatus status; // Trạng thái duyệt và thực hiện.
    private Long periodId; // Mã đợt đăng ký chứa đề tài.

    public StudentTopic(Long id, String title, String description, String department,
            String supervisor, String coSupervisor, Long periodId) { // Khởi tạo đề tài ở trạng thái nháp.
        this.id = id; // Gán mã đề tài.
        this.title = title; // Gán tên đề tài.
        this.description = description; // Gán mô tả đề tài.
        this.department = department; // Gán bộ môn phụ trách.
        this.supervisor = supervisor; // Gán GV hướng dẫn chính.
        this.coSupervisor = coSupervisor; // Gán GV hướng dẫn phụ.
        this.periodId = periodId; // Liên kết đề tài với đợt.
        this.status = TopicStatus.DRAFT; // Đề tài mới tạo chưa công bố.
    }

    public StudentTopic() {
    } // Constructor rỗng để Jackson dựng lại đề tài đã được lưu.

    public Long getId() {
        return id;
    } // Trả về mã đề tài.

    public String getTitle() {
        return title;
    } // Trả về tên đề tài.

    public String getDescription() {
        return description;
    } // Trả về mô tả.

    public String getDepartment() {
        return department;
    } // Trả về bộ môn.

    public String getSupervisor() {
        return supervisor;
    } // Trả về GV hướng dẫn chính.

    public String getCoSupervisor() {
        return coSupervisor;
    } // Trả về GV hướng dẫn phụ.

    public TopicStatus getStatus() {
        return status;
    } // Trả về trạng thái hiện tại.

    public Long getPeriodId() {
        return periodId;
    } // Trả về mã đợt.

    public void setStatus(TopicStatus status) {
        this.status = status;
    } // Cập nhật trạng thái đề tài.

    public void updateDetails(String title, String description, String department) {
        this.title = title; // Cập nhật thông tin giáo viên được phép hiệu chỉnh.
        this.description = description;
        this.department = department;
    } // Cập nhật nội dung nhưng giữ nguyên giảng viên hướng dẫn.

    public void updateDetails(String title, String description, String department,
            String supervisor, Long periodId) {
        updateDetails(title, description, department);
        this.supervisor = supervisor;
        this.periodId = periodId;
    } // Phiên bản dành riêng cho khoa: đổi người hướng dẫn và đợt liên kết.
}