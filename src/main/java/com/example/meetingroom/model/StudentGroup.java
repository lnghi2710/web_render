package com.example.meetingroom.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

// Bảo đảm các trường nhóm/điểm có thể được khôi phục từ JSON khi ứng dụng khởi động lại.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StudentGroup { // Nhóm sinh viên đăng ký thực hiện đề tài.
    private Long id; // Mã định danh của nhóm.
    private String name; // Tên nhóm hiển thị.
    private String leader; // Tên và MSSV nhóm trưởng.
    private String members; // Danh sách thành viên phân cách bằng dấu phẩy.
    private Long topicId; // Mã đề tài nhóm đăng ký.
    private String status; // Trạng thái duyệt, thực hiện hoặc công bố.
    private boolean reportSubmitted; // Đánh dấu nhóm đã nộp báo cáo.
    private Double reviewerScore; // Điểm giảng viên phản biện.
    private Double councilScore; // Điểm hội đồng.

    public StudentGroup(Long id, String name, String leader, String members, Long topicId) { // Tạo nhóm mới.
        this.id = id; // Lưu mã nhóm.
        this.name = name; // Lưu tên nhóm.
        this.leader = leader; // Lưu nhóm trưởng.
        this.members = members; // Lưu danh sách thành viên.
        this.topicId = topicId; // Liên kết nhóm với đề tài.
        this.status = "Chờ duyệt"; // Nhóm mới cần giáo viên phê duyệt.
    }

    public StudentGroup() {
    } // Constructor rỗng dành cho Jackson khi đọc dữ liệu đã lưu.

    public Long getId() {
        return id;
    } // Trả mã nhóm cho giao diện.

    public String getName() {
        return name;
    } // Trả tên nhóm.

    public String getLeader() {
        return leader;
    } // Trả nhóm trưởng.

    public String getMembers() {
        return members;
    } // Trả danh sách thành viên.

    public Long getTopicId() {
        return topicId;
    } // Trả mã đề tài liên kết.

    public String getStatus() {
        return status;
    } // Trả trạng thái nhóm.

    public boolean isReportSubmitted() {
        return reportSubmitted;
    } // Kiểm tra trạng thái nộp báo cáo.

    public Double getReviewerScore() {
        return reviewerScore;
    } // Trả điểm GVPB.

    public Double getCouncilScore() {
        return councilScore;
    } // Trả điểm hội đồng.

    public Double getFinalScore() { // Tính trung bình cộng hai điểm thành phần.
        if (reviewerScore == null || councilScore == null)
            return null; // Chưa đủ điểm thì chưa có kết quả.
        return (reviewerScore + councilScore) / 2.0; // Trả điểm cuối cùng theo thang 10.
    }

    public void setStatus(String status) {
        this.status = status;
    } // Cập nhật trạng thái nhóm.

    public void setReportSubmitted(boolean reportSubmitted) {
        this.reportSubmitted = reportSubmitted;
    } // Cập nhật trạng thái báo cáo.

    public void setReviewerScore(Double reviewerScore) {
        this.reviewerScore = reviewerScore;
    } // Lưu điểm GVPB.

    public void setCouncilScore(Double councilScore) {
        this.councilScore = councilScore;
    } // Lưu điểm hội đồng.
}