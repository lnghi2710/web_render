package com.example.meetingroom;

@Deprecated // Giữ tương thích với nút debug cũ còn cache trong VS Code.
public final class MeetingRoomBookingApplication {
    private MeetingRoomBookingApplication() {
    } // Không cho tạo instance của lớp chuyển tiếp.

    @Deprecated
    public static void main(String[] args) { // Điểm vào cho cấu hình debug cũ.
        StudentTopicManagementApplication.main(args); // Chạy ứng dụng quản lý đề tài hiện tại.
    }
}