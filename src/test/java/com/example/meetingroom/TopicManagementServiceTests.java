package com.example.meetingroom;

import com.example.meetingroom.model.UserAccount;
import com.example.meetingroom.model.UserRole;
import com.example.meetingroom.service.AuthService;
import com.example.meetingroom.service.TopicManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TopicManagementServiceTests {
    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path temporaryDirectory;

    @Test
    // Kiểm tra đồng thời giới hạn đăng ký và khôi phục cả dữ liệu đã ghi ra đĩa.
    void topicRegistrationLimitAndSavedDataSurviveReload() {
        Path dataFile = temporaryDirectory.resolve("topics.json");
        TopicManagementService service = new TopicManagementService(objectMapper, dataFile.toString());
        service.initialize(); // Tạo bộ dữ liệu mẫu riêng trong thư mục tạm của bài test.

        // Tạo đề tài thử nghiệm mới để kiểm thử giới hạn 5 người mà không phụ thuộc đề tài demo đã có nhóm.
        UserAccount teacher = new UserAccount("gv.test", "123456", "Giảng viên kiểm thử",
                null, UserRole.TEACHER);
        UserAccount facultyHead = new UserAccount("bm.test", "123456", "Trưởng bộ môn kiểm thử",
                null, UserRole.DEPARTMENT_HEAD);
        service.createTopic("Đề tài thử nghiệm", "Nội dung có thể chỉnh sửa", "Công nghệ phần mềm",
                teacher, 1L);
        assertThrows(IllegalArgumentException.class, () -> service.publishTopic(10L));
        assertThrows(IllegalArgumentException.class, () -> service.approveTopic(10L, teacher));
        service.approveTopic(10L, facultyHead);
        service.publishTopic(10L);
        UserAccount student = new UserAccount("30123456", "123456", "Sinh viên kiểm thử",
                "30123456", UserRole.STUDENT);
        service.createGroup("SE-05", "30123456", "30123456, 30123457, 30123458, 30123459, 30123460",
                10L, student);

        assertEquals(5, service.registeredStudentCount(10L));
        assertTrue(service.availableTopics().isEmpty());

        Long groupId = service.groups().stream()
                .filter(group -> group.getTopicId().equals(10L))
                .findFirst().orElseThrow().getId();
        assertThrows(IllegalArgumentException.class, () -> service.approveGroup(groupId, teacher));
        service.approveGroup(groupId, facultyHead);
        service.submitReport(groupId, student);
        UserAccount reviewer = new UserAccount("gv.pb", "123456", "Giảng viên phản biện",
                null, UserRole.REVIEWER);
        assertThrows(IllegalArgumentException.class, () -> service.grade(groupId, 8.5, 9.0, reviewer));
        assertThrows(IllegalArgumentException.class, () -> service.grade(groupId, 8.5, 9.0, teacher));
        service.grade(groupId, 8.5, 9.0, facultyHead);
        TopicManagementService reloaded = new TopicManagementService(objectMapper, dataFile.toString());
        reloaded.initialize();
        assertEquals(5, reloaded.registeredStudentCount(10L));
        assertEquals(8.75, reloaded.groups().stream()
                .filter(group -> group.getTopicId().equals(10L))
                .findFirst().orElseThrow().getFinalScore());
        assertTrue(reloaded.availableTopics().isEmpty());
    }

    @Test
    // Yêu cầu trực tiếp tới service cũng không thể vượt giới hạn của biểu mẫu.
    void registrationCannotExceedFiveStudents() {
        TopicManagementService service = new TopicManagementService(
                objectMapper, temporaryDirectory.resolve("capacity.json").toString());
        service.initialize();
        UserAccount teacher = new UserAccount("gv.test", "123456", "Giảng viên kiểm thử",
                null, UserRole.TEACHER);
        UserAccount facultyHead = new UserAccount("bm.test", "123456", "Trưởng bộ môn kiểm thử",
                null, UserRole.DEPARTMENT_HEAD);
        service.createTopic("Đề tài thử nghiệm", "Nội dung có thể chỉnh sửa", "Công nghệ phần mềm",
                teacher, 1L);
        service.approveTopic(10L, facultyHead);
        service.publishTopic(10L);
        UserAccount student = new UserAccount("30123456", "123456", "Sinh viên mới",
                "30123456", UserRole.STUDENT);

        assertThrows(IllegalArgumentException.class, () -> service.createGroup("SE-06", "30123456",
                "30123456,30123457,30123458,30123459,30123460,30123461", 10L, student));
    }

    @Test
    void registrationEnforcesGroupSizeMembershipLeaderAndAdvisorScoringRules() {
        TopicManagementService service = new TopicManagementService(
                objectMapper, temporaryDirectory.resolve("group-rules.json").toString());
        service.initialize();
        UserAccount advisor = new UserAccount("gv.test", "123456", "Giảng viên kiểm thử",
                null, UserRole.SUPERVISOR);
        UserAccount facultyHead = new UserAccount("bm.test", "123456", "Trưởng bộ môn kiểm thử",
                null, UserRole.DEPARTMENT_HEAD);
        service.createTopic("Đề tài kiểm tra nhóm", "Mô tả", "Công nghệ phần mềm", advisor, 1L);
        service.approveTopic(10L, facultyHead);
        service.publishTopic(10L);

        UserAccount leader = new UserAccount("30123456", "pw", "Nhóm trưởng", "30123456", UserRole.STUDENT);
        String members = "30123456,30123457,30123458";
        service.createGroup("SE-07", "30123456", members, 10L, leader);
        Long groupId = service.groups().stream().filter(group -> group.getTopicId().equals(10L))
                .findFirst().orElseThrow().getId();

        assertThrows(IllegalArgumentException.class,
                () -> service.createGroup("SE-08", "30123456", "30123456,30123457", 10L, leader));
        assertThrows(IllegalArgumentException.class,
                () -> service.createGroup("SE-08", "30123460", members, 10L, leader));
        assertThrows(IllegalArgumentException.class, () -> service.approveGroup(groupId, advisor));
        service.approveGroup(groupId, facultyHead);
        assertThrows(IllegalArgumentException.class,
                () -> service.submitReport(groupId,
                        new UserAccount("30123457", "pw", "Thành viên", "30123457", UserRole.STUDENT)));
        service.submitReport(groupId, leader);

        UserAccount supervisorAsReviewer = new UserAccount("gv.pb", "pw", "Giảng viên kiểm thử",
                null, UserRole.REVIEWER);
        assertThrows(IllegalArgumentException.class,
                () -> service.grade(groupId, 8.0, 9.0, supervisorAsReviewer));

        service.createTopic("Đề tài thứ hai", "Mô tả", "Công nghệ phần mềm", advisor, 1L);
        Long secondTopicId = service.topics().stream().filter(topic -> topic.getTitle().equals("Đề tài thứ hai"))
                .findFirst().orElseThrow().getId();
        service.approveTopic(secondTopicId, facultyHead);
        service.publishTopic(secondTopicId);
        assertThrows(IllegalArgumentException.class,
                () -> service.createGroup("SE-09", "30123456", members, secondTopicId, leader));
    }

    @Test
    void notificationsArePersistedAndPrivateToTheirRecipient() {
        Path dataFile = temporaryDirectory.resolve("notifications.json");
        TopicManagementService service = new TopicManagementService(objectMapper, dataFile.toString());
        service.initialize();
        UserAccount student = new UserAccount("21161067", "pw", "SV", "21161067", UserRole.STUDENT);
        UserAccount anotherStudent = new UserAccount("20146298", "pw", "SV khác", "20146298", UserRole.STUDENT);
        UserAccount teacher = new UserAccount("gv.test", "pw", "Giảng viên", null, UserRole.TEACHER);
        service.createAnnouncement("Lịch bảo vệ", "Lịch được cập nhật", List.of(student.getUsername()), teacher);
        var shared = service.notificationsFor(student).get(0);
        assertEquals("Lịch bảo vệ", shared.getTitle());
        assertTrue(service.notificationsFor(anotherStudent).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.createAnnouncement("Thông báo", "Nội dung", List.of(student.getUsername()), student));
        assertThrows(IllegalArgumentException.class,
                () -> service.markNotificationRead(shared.getId(), student));

        service.createAnnouncement("Điểm đã công bố", "Thông báo cá nhân", List.of(student.getUsername()), teacher);
        var personal = service.notificationsFor(student).stream()
                .filter(item -> item.getTitle().equals("Điểm đã công bố")).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.markNotificationRead(personal.getId(), anotherStudent));

        TopicManagementService restored = new TopicManagementService(objectMapper, dataFile.toString());
        restored.initialize();
        assertTrue(restored.notificationsFor(student).stream()
                .anyMatch(item -> item.getId().equals(personal.getId()) && !item.isRead()));
    }

    @Test
    void adminCrudPersistsAndProtectsLinkedRecords() {
        Path dataFile = temporaryDirectory.resolve("crud-topics.json");
        TopicManagementService service = new TopicManagementService(objectMapper, dataFile.toString());
        service.initialize();
        UserAccount teacher = new UserAccount("gv.test", "123456", "Giảng viên kiểm thử",
                null, UserRole.TEACHER);

        service.createTopic("Đề tài mới", "Mô tả mới", "Công nghệ phần mềm", teacher, 1L);
        service.updateTopicAsAdmin(10L, "Đề tài đã sửa", "Nội dung đã sửa", "Hệ thống thông tin", teacher, 1L);
        assertEquals("Giảng viên kiểm thử", service.topics().stream()
                .filter(topic -> topic.getId().equals(10L)).findFirst().orElseThrow().getSupervisor());
        assertThrows(IllegalArgumentException.class, () -> service.deleteTopic(1L));
        assertThrows(IllegalArgumentException.class, () -> service.deletePeriod(1L));

        service.deleteTopic(10L);
        TopicManagementService restored = new TopicManagementService(objectMapper, dataFile.toString());
        restored.initialize();
        assertTrue(restored.topics().stream().noneMatch(topic -> topic.getId().equals(10L)));
        assertEquals("KLTN học kỳ 1 - 2026", restored.periods().get(0).getName());
    }

    @Test
    void accountChangesPersistAndAdminCannotBeDeleted() {
        Path accountFile = temporaryDirectory.resolve("accounts.json");
        AuthService auth = new AuthService(objectMapper, accountFile.toString());
        auth.initialize();
        auth.createAccount("30123456", "pw", "Sinh viên mới", "30123456", UserRole.STUDENT);
        auth.createAccount("gv.moi", "pw", "Giáo viên mới", null, UserRole.SUPERVISOR);
        assertThrows(IllegalArgumentException.class, () -> auth.deleteAccount("admin.khoa"));

        auth.deleteAccount("30123456");
        AuthService restored = new AuthService(objectMapper, accountFile.toString());
        restored.initialize();
        assertNull(restored.authenticate("30123456", "pw"));
        assertEquals("Giáo viên mới", restored.authenticate("gv.moi", "pw").getDisplayName());
        assertTrue(restored.authenticate("gv.moi", "pw").getPasswordHash().startsWith("$2"));
        assertThrows(IllegalArgumentException.class, () -> restored.createAccount(
                "21161067", "pw", "Trùng MSSV", "21161067", UserRole.STUDENT));
    }
}
