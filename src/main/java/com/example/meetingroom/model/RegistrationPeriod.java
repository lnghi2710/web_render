package com.example.meetingroom.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.LocalDate;

// Cho phép nạp lại các mốc ngày và trạng thái đợt từ kho JSON của ứng dụng.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RegistrationPeriod { // Thông tin một đợt đăng ký đề tài.
    private Long id; // Mã duy nhất của đợt.
    private String name; // Tên đợt đăng ký.
    private ProjectType type; // Loại đề tài của đợt.
    private LocalDate teacherStart; // Ngày mở đăng ký dành cho giáo viên.
    private LocalDate teacherEnd; // Ngày kết thúc đăng ký dành cho giáo viên.
    private LocalDate studentStart; // Ngày mở đăng ký dành cho sinh viên.
    private LocalDate studentEnd; // Ngày kết thúc đăng ký dành cho sinh viên.
    private LocalDate reviewerDeadline; // Hạn GVPB gửi điểm.
    private LocalDate defenseDate; // Ngày báo cáo hội đồng, dùng cho KLTN.
    private boolean published; // Cho biết đợt đã được công bố chưa.

    public RegistrationPeriod(Long id, String name, ProjectType type, LocalDate teacherStart,
            LocalDate teacherEnd, LocalDate studentStart, LocalDate studentEnd,
            LocalDate reviewerDeadline, LocalDate defenseDate) { // Tạo đợt với các mốc đăng ký và đánh giá.
        this.id = id; // Lưu mã đợt.
        this.name = name; // Lưu tên đợt.
        this.type = type; // Lưu loại đợt.
        this.teacherStart = teacherStart; // Lưu ngày bắt đầu đăng ký của GV.
        this.teacherEnd = teacherEnd; // Lưu ngày kết thúc đăng ký của GV.
        this.studentStart = studentStart; // Lưu ngày bắt đầu đăng ký của SV.
        this.studentEnd = studentEnd; // Lưu ngày kết thúc đăng ký của SV.
        this.reviewerDeadline = reviewerDeadline; // Lưu hạn nộp điểm phản biện.
        this.defenseDate = defenseDate; // Lưu ngày hội đồng.
    }

    public RegistrationPeriod() {
    } // Constructor rỗng dành cho Jackson khi đọc đợt đăng ký đã lưu.

    public void updateDetails(String name, ProjectType type, LocalDate teacherStart, LocalDate teacherEnd,
            LocalDate studentStart, LocalDate studentEnd, LocalDate reviewerDeadline, LocalDate defenseDate) {
        this.name = name;
        this.type = type;
        this.teacherStart = teacherStart;
        this.teacherEnd = teacherEnd;
        this.studentStart = studentStart;
        this.studentEnd = studentEnd;
        this.reviewerDeadline = reviewerDeadline;
        this.defenseDate = defenseDate;
    } // Cập nhật các trường do khoa quản lý, giữ nguyên mã và trạng thái công bố.

    public Long getId() {
        return id;
    } // Trả về mã đợt.

    public String getName() {
        return name;
    } // Trả về tên đợt.

    public ProjectType getType() {
        return type;
    } // Trả về loại đợt.

    public LocalDate getTeacherStart() {
        return teacherStart;
    } // Trả về ngày mở đăng ký GV.

    public LocalDate getTeacherEnd() {
        return teacherEnd;
    } // Trả về ngày đóng đăng ký GV.

    public LocalDate getStudentStart() {
        return studentStart;
    } // Trả về ngày mở đăng ký SV.

    public LocalDate getStudentEnd() {
        return studentEnd;
    } // Trả về ngày đóng đăng ký SV.

    public LocalDate getReviewerDeadline() {
        return reviewerDeadline;
    } // Trả về hạn GVPB.

    public LocalDate getDefenseDate() {
        return defenseDate;
    } // Trả về ngày hội đồng.

    public boolean isPublished() {
        return published;
    } // Kiểm tra trạng thái công bố.

    public void setPublished(boolean published) {
        this.published = published;
    } // Cập nhật trạng thái công bố.
}