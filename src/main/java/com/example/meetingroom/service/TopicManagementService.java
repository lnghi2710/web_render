package com.example.meetingroom.service;// Lớp dịch vụ quản lý đề tài, nhóm và đợt đăng ký; lưu trữ dữ liệu vào tệp JSON.

import com.example.meetingroom.model.ProjectType;// Loại đề tài: TLCN, KLTN, Đồ án môn học.
import com.example.meetingroom.model.NotificationItem;// Thông báo chung hoặc cá nhân, trạng thái đọc/chưa đọc.
import com.example.meetingroom.model.RegistrationPeriod;// Đợt đăng ký đề tài, mốc thời gian và trạng thái công bố.
import com.example.meetingroom.model.StudentGroup;// Nhóm sinh viên đăng ký đề tài, danh sách thành viên và trạng thái duyệt.
import com.example.meetingroom.model.StudentTopic;// Đề tài và nhóm sinh viên đăng ký.
import com.example.meetingroom.model.TopicStatus;// Trạng thái đề tài: bản nháp, đã duyệt, đã công bố, đã có nhóm, đã hoàn thành.
import com.example.meetingroom.model.UserAccount; // Dữ liệu tài khoản và vai trò trong phiên đăng nhập.
import com.example.meetingroom.model.UserRole;
import org.springframework.beans.factory.annotation.Value;// Lấy đường dẫn tệp JSON từ application.properties, mặc định là data/student-topic-data.json.
import org.springframework.stereotype.Service;// Lớp dịch vụ quản lý đề tài, nhóm và đợt đăng ký; lưu trữ dữ liệu vào tệp JSON.
import tools.jackson.core.JacksonException;// Lỗi đọc/ghi JSON do Spring Boot cấu hình, hỗ trợ ngày tháng và enum.
import tools.jackson.databind.ObjectMapper;// Bộ đọc/ghi JSON do Spring Boot cấu hình, hỗ trợ ngày tháng và enum.

import jakarta.annotation.PostConstruct;// Khởi tạo dữ liệu mẫu hoặc nạp dữ liệu từ tệp JSON khi ứng dụng khởi động.
import java.io.IOException;// Đọc/ghi tệp JSON lưu trữ dữ liệu đề tài, nhóm và đợt đăng ký.
import java.nio.file.AtomicMoveNotSupportedException;// Đọc/ghi tệp JSON lưu trữ dữ liệu đề tài, nhóm và đợt đăng ký.
import java.nio.file.Files;// Đọc/ghi tệp JSON lưu trữ dữ liệu đề tài, nhóm và đợt đăng ký.
import java.nio.file.Path;// Đọc/ghi tệp JSON lưu trữ dữ liệu đề tài, nhóm và đợt đăng ký.
import java.nio.file.StandardCopyOption;// Đọc/ghi tệp JSON lưu trữ dữ liệu đề tài, nhóm và đợt đăng ký.
import java.time.LocalDate;// Ngày tháng dùng để kiểm tra mốc thời gian đăng ký và nộp báo cáo.
import java.util.ArrayList; // Danh sách bộ môn, đợt đăng ký, đề tài, nhóm và thông báo đang hoạt động trong bộ nhớ.
import java.util.List; // Danh sách bộ môn mặc định để sinh viên và giáo viên chọn khi tạo đề tài.
import java.util.concurrent.atomic.AtomicLong; // Cấp mã duy nhất cho đề tài, nhóm và thông báo.

@Service // Lớp dịch vụ quản lý đề tài, nhóm và đợt đăng ký; lưu trữ dữ liệu vào tệp JSON.
public class TopicManagementService {
    public static final int MAX_STUDENTS_PER_TOPIC = 5; // Giới hạn sĩ số được kiểm tra ở tầng nghiệp vụ.
    // Danh sách lựa chọn dùng chung cho form và kiểm tra dữ liệu gửi từ máy khách.
    private static final List<String> DEFAULT_DEPARTMENTS = List.of(
            "Công nghệ phần mềm", "Hệ thống thông tin", "Khoa học máy tính",
            "Mạng máy tính và truyền thông", "Kỹ thuật máy tính");
    private final List<String> departments = new ArrayList<>(DEFAULT_DEPARTMENTS);// Danh sách bộ môn, đợt đăng ký, đề tài, nhóm và thông báo đang hoạt động trong bộ nhớ.
    private final List<RegistrationPeriod> periods = new ArrayList<>(); // Dữ liệu đang hoạt động trong bộ nhớ.
    private final List<StudentTopic> topics = new ArrayList<>(); // Đề tài đã tạo.
    private final List<StudentGroup> groups = new ArrayList<>(); // Nhóm đã đăng ký.
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(10); // Cấp mã mới, không trùng dữ liệu mẫu.
    private final ObjectMapper dataMapper; // Bộ đọc/ghi JSON do Spring Boot cấu hình.
    private final Path dataFile; // Tệp lưu bền vững, mặc định ở thư mục data của ứng dụng.

    public TopicManagementService(ObjectMapper objectMapper, // Bộ đọc/ghi JSON do Spring Boot cấu hình.
            @Value("${app.data-file:data/student-topic-data.json}") String dataFile) { // Lấy đường dẫn tệp JSON từ application.properties, mặc định là data/student-topic-data.json.
        this.dataMapper = objectMapper; // Tái sử dụng mapper để giữ hỗ trợ ngày tháng và enum.
        this.dataFile = Path.of(dataFile).toAbsolutePath().normalize(); // Chuẩn hóa đường dẫn lưu dữ liệu.
    }

    @PostConstruct// Khởi tạo dữ liệu mẫu hoặc nạp dữ liệu từ tệp JSON khi ứng dụng khởi động.
    public synchronized void initialize() {
        if (Files.exists(dataFile)) {
            load(); // Có dữ liệu thì khôi phục, tránh tạo lại dữ liệu mẫu ghi đè dữ liệu người
                    // dùng.
        } else {
            seedDemoData(); // Chỉ tạo dữ liệu ban đầu một lần trên lần chạy đầu.
            save(); // Lưu ngay bộ dữ liệu khởi tạo để lần chạy sau có thể phục hồi.
        }
    }

    // Trả bản sao bất biến để lớp gọi không sửa trực tiếp trạng thái nội bộ.
    public synchronized List<RegistrationPeriod> periods() { 
        return List.copyOf(periods); 
    }

    public synchronized List<StudentTopic> topics() { 
        return topics.stream().sorted((left, right) -> Long.compare(left.getId(), right.getId())).toList();
    }

    public synchronized List<StudentGroup> groups() {
        return List.copyOf(groups);
    }

    // Cấp danh mục cho select box, không để giáo viên nhập bộ môn tùy ý.
    public List<String> departments() {
        return List.copyOf(departments);
    }

    public synchronized void createDepartment(String name) {
        String normalized = cleanDepartmentName(name);
        if (departments.stream().anyMatch(item -> item.equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException("Bộ môn đã tồn tại");
        }
        departments.add(normalized);
        save();
    }

    public synchronized void updateDepartment(String currentName, String newName) {
        String current = cleanDepartmentName(currentName);
        String updated = cleanDepartmentName(newName);
        if (departments.stream().noneMatch(item -> item.equalsIgnoreCase(current))) {
            throw new IllegalArgumentException("Không tìm thấy bộ môn");
        }
        if (departments.stream().anyMatch(item -> !item.equalsIgnoreCase(current)
                && item.equalsIgnoreCase(updated))) {
            throw new IllegalArgumentException("Tên bộ môn đã được sử dụng");
        }
        departments.replaceAll(item -> item.equalsIgnoreCase(current) ? updated : item);
        topics.stream().filter(topic -> topic.getDepartment().equalsIgnoreCase(current))
                .forEach(topic -> topic.updateDetails(topic.getTitle(), topic.getDescription(), updated));
        save();
    }

    public synchronized void deleteDepartment(String name) {
        String normalized = cleanDepartmentName(name);
        if (topics.stream().anyMatch(topic -> topic.getDepartment().equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException("Không thể xóa bộ môn đang có đề tài");
        }
        if (!departments.removeIf(item -> item.equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException("Không tìm thấy bộ môn");
        }
        save();
    }

    private String cleanDepartmentName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 150) {
            throw new IllegalArgumentException("Tên bộ môn là bắt buộc và tối đa 150 ký tự");
        }
        return name.trim();
    }

    // Giáo viên và khoa theo dõi toàn bộ nhóm; sinh viên chỉ thấy nhóm có mã của
    // mình.
    public synchronized List<StudentGroup> groupsFor(UserAccount user) {
        if (user == null) {
            return List.of();
        }
        if (user.isTeacher()) {
            return groups();
        }
        return groups.stream()
                .filter(group -> memberIds(group.getMembers()).stream()
                        .anyMatch(member -> member.equals(user.getStudentId())
                                || member.startsWith(user.getStudentId() + " - ")))
                .toList();
    }

    public synchronized List<StudentGroup> groupsFor(UserAccount user, Long periodId) {
        if (user == null) {
            return List.of();
        }
        return groups.stream()
                .filter(group -> periodId == null || findTopic(group.getTopicId()).getPeriodId().equals(periodId))
                .filter(group -> !user.isStudent()
                        || memberIds(group.getMembers()).contains(user.getStudentId()))
                .toList();
    }

    public synchronized List<NotificationItem> notificationsFor(UserAccount user) {
        if (user == null) {
            return List.of();
        }
        return notifications.stream()
                .filter(item -> item.getRecipientUsername() == null
                        || item.getRecipientUsername().equalsIgnoreCase(user.getUsername()))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
    }

    public synchronized void markNotificationRead(Long id, UserAccount user) {
        NotificationItem notification = notifications.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        if (user == null || user.isStudent() || (notification.getRecipientUsername() != null
                && !notification.getRecipientUsername().equalsIgnoreCase(user.getUsername()))) {
            throw new IllegalArgumentException("Bạn không có quyền cập nhật trạng thái thông báo này");
        }
        notification.markRead();
        save();
    }

    public synchronized void createAnnouncement(String title, String message, List<String> recipientUsernames,
            UserAccount sender) {
        if (sender == null || !sender.isTeacher()) {
            throw new IllegalArgumentException("Chỉ giảng viên và admin được gửi thông báo");
        }
        if (title == null || title.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Tiêu đề và nội dung thông báo là bắt buộc");
        }
        if (recipientUsernames == null || recipientUsernames.isEmpty()
                || recipientUsernames.stream().anyMatch(username -> username == null || username.isBlank())) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một người nhận");
        }
        List<String> recipients = new ArrayList<>();
        for (String username : recipientUsernames) {
            String normalized = username.trim();
            if (recipients.stream().noneMatch(existing -> existing.equalsIgnoreCase(normalized))) {
                recipients.add(normalized);
            }
        }
        for (String recipient : recipients) {
            addNotification(recipient, title.trim(), message.trim());
        }
        save();
    }

    public synchronized void deleteAnnouncement(Long id) {
        NotificationItem notification = notifications.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        if (notification.getRecipientUsername() != null) {
            throw new IllegalArgumentException("Chỉ có thể xóa thông báo chung do quản trị tạo");
        }
        notifications.remove(notification);
        save();
    }

    public synchronized List<StudentTopic> searchTopics(String keyword, String department, TopicStatus status,
            Long periodId, String supervisor) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        String supervisorQuery = supervisor == null ? "" : supervisor.trim().toLowerCase(java.util.Locale.ROOT);
        return topics.stream()
                .filter(topic -> query.isEmpty()
                        || topic.getTitle().toLowerCase(java.util.Locale.ROOT).contains(query)
                        || topic.getDescription().toLowerCase(java.util.Locale.ROOT).contains(query))
                .filter(topic -> department == null || department.isBlank()
                        || topic.getDepartment().equals(department))
                .filter(topic -> status == null || topic.getStatus() == status)
                .filter(topic -> periodId == null || topic.getPeriodId().equals(periodId))
                .filter(topic -> supervisorQuery.isEmpty()
                        || topic.getSupervisor().toLowerCase(java.util.Locale.ROOT).contains(supervisorQuery))
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();
    }

    public synchronized int registeredStudentCount(Long topicId) {
        return groups.stream()
                .filter(group -> group.getTopicId().equals(topicId))
                // Thành viên được nhập theo định dạng "MSSV - Họ tên", nên đếm từng mục phân
                // cách bằng dấu phẩy.
                .mapToInt(group -> memberIds(group.getMembers()).size())
                .sum();
    }

    // Chỉ đề tài đã công bố, chưa có nhóm và còn chỗ mới xuất hiện trong lựa chọn
    // của sinh viên.
    public synchronized List<StudentTopic> availableTopics() {
        return topics.stream()
                .filter(topic -> topic.getStatus() == TopicStatus.PUBLISHED)
                .filter(topic -> periods.stream().anyMatch(period -> period.getId().equals(topic.getPeriodId())
                        && period.isPublished()
                        && !LocalDate.now().isBefore(period.getStudentStart())
                        && !LocalDate.now().isAfter(period.getStudentEnd())))
                .filter(topic -> registeredStudentCount(topic.getId()) < MAX_STUDENTS_PER_TOPIC)
                .filter(topic -> groups.stream().noneMatch(group -> group.getTopicId().equals(topic.getId())))
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();
    }

    public synchronized void createPeriod(String name, ProjectType type, LocalDate teacherStart,
            LocalDate teacherEnd, LocalDate studentStart, LocalDate studentEnd,
            LocalDate reviewerDeadline, LocalDate defenseDate) {
        validatePeriod(name, type, teacherStart, teacherEnd, studentStart, studentEnd, reviewerDeadline, defenseDate);
        periods.add(new RegistrationPeriod(ids.getAndIncrement(), name.trim(), type, teacherStart, teacherEnd,
                studentStart, studentEnd, reviewerDeadline, defenseDate));
        save(); // Ghi thay đổi ngay sau khi nghiệp vụ hợp lệ.
    }

    // Chỉ cập nhật đợt sau khi kiểm tra lại toàn bộ mốc thời gian ở tầng nghiệp vụ.
    public synchronized void updatePeriod(Long id, String name, ProjectType type, LocalDate teacherStart,
            LocalDate teacherEnd, LocalDate studentStart, LocalDate studentEnd,
            LocalDate reviewerDeadline, LocalDate defenseDate) {
        validatePeriod(name, type, teacherStart, teacherEnd, studentStart, studentEnd, reviewerDeadline, defenseDate);
        findPeriod(id).updateDetails(name.trim(), type, teacherStart, teacherEnd, studentStart, studentEnd,
                reviewerDeadline, defenseDate);
        save();
    }

    // Không xóa đợt khi còn đề tài liên kết để tránh tạo dữ liệu mồ côi.
    public synchronized void deletePeriod(Long id) {
        findPeriod(id);
        if (topics.stream().anyMatch(topic -> topic.getPeriodId().equals(id))) {
            throw new IllegalArgumentException("Không thể xóa đợt đăng ký đang có đề tài liên kết");
        }
        periods.removeIf(period -> period.getId().equals(id));
        save();
    }

    private void validatePeriod(String name, ProjectType type, LocalDate teacherStart, LocalDate teacherEnd,
            LocalDate studentStart, LocalDate studentEnd, LocalDate reviewerDeadline, LocalDate defenseDate) {
        if (name == null || name.isBlank() || type == null || teacherStart == null || teacherEnd == null
                || studentStart == null || studentEnd == null) {
            throw new IllegalArgumentException("Vui lòng nhập đủ tên đợt, loại đề tài và các mốc đăng ký");
        }
        if (!teacherEnd.isAfter(teacherStart) || !studentEnd.isAfter(studentStart)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if ((type == ProjectType.TLCN || type == ProjectType.KLTN) && reviewerDeadline == null) {
            throw new IllegalArgumentException("TLCN/KLTN phải có hạn nộp điểm phản biện");
        }
        if (type == ProjectType.KLTN && defenseDate == null) {
            throw new IllegalArgumentException("KLTN phải có ngày báo cáo hội đồng");
        }
    }

    public synchronized void createTopic(String title, String description, String department,
            UserAccount supervisor, Long periodId) {
        // Dù client sửa request, chỉ bộ môn trong danh mục mới được lưu.
        if (title == null || title.isBlank() || description == null || description.isBlank()
                || !departments.contains(department)) {
            throw new IllegalArgumentException("Tên, nội dung và bộ môn là bắt buộc");
        }
        // Không nhận tên giảng viên từ form; controller đưa vào tài khoản đang đăng
        // nhập.
        if (supervisor == null || !supervisor.canSupervise()) {
            throw new IllegalArgumentException("Giảng viên hướng dẫn không hợp lệ");
        }
        if (periods.stream().noneMatch(period -> period.getId().equals(periodId))) {
            throw new IllegalArgumentException("Vui lòng chọn đợt đăng ký hợp lệ");
        }
        RegistrationPeriod period = findPeriod(periodId);
        if (LocalDate.now().isBefore(period.getTeacherStart()) || LocalDate.now().isAfter(period.getTeacherEnd())) {
            throw new IllegalArgumentException("Đợt hiện không mở giai đoạn đăng ký đề tài dành cho giảng viên");
        }
        topics.add(new StudentTopic(ids.getAndIncrement(), title.trim(), description.trim(), department.trim(),
                supervisor.getDisplayName(), "", periodId));
        addNotification(null, "Đề tài mới", "Đề tài \"" + title.trim() + "\" vừa được gửi để duyệt.");
        save(); // Lưu cả tên GV lấy từ hồ sơ tài khoản.
    }

    // Cho phép tinh chỉnh nội dung đề tài, không thay đổi người hướng dẫn đã được
    // khóa khi tạo.
    public synchronized void updateTopic(Long id, String title, String description, String department) {
        validateTopicDetails(title, description, department);
        findTopic(id).updateDetails(title.trim(), description.trim(), department.trim());
        save();
    }

    public synchronized void updateTopic(Long id, String title, String description, String department,
            UserAccount editor) {
        StudentTopic topic = findTopic(id);
        if (editor == null || !editor.canSupervise()
                || !topic.getSupervisor().equalsIgnoreCase(editor.getDisplayName())) {
            throw new IllegalArgumentException("Chỉ giảng viên hướng dẫn của đề tài mới được chỉnh sửa");
        }
        validateTopicDetails(title, description, department);
        topic.updateDetails(title.trim(), description.trim(), department.trim());
        save();
    }

    // Quyền khoa có thể đổi người hướng dẫn và đợt, còn giáo viên chỉ sửa nội dung
    // đề tài.
    public synchronized void updateTopicAsAdmin(Long id, String title, String description, String department,
            UserAccount supervisor, Long periodId) {
        validateTopicDetails(title, description, department);
        if (supervisor == null || !supervisor.canSupervise()) {
            throw new IllegalArgumentException("Giảng viên hướng dẫn không hợp lệ");
        }
        if (periods.stream().noneMatch(period -> period.getId().equals(periodId))) {
            throw new IllegalArgumentException("Vui lòng chọn đợt đăng ký hợp lệ");
        }
        findTopic(id).updateDetails(title.trim(), description.trim(), department.trim(),
                supervisor.getDisplayName(), periodId);
        save();
    }

    // Bảo toàn lịch sử đăng ký: đề tài có nhóm liên quan không được xóa.
    public synchronized void deleteTopic(Long id) {
        findTopic(id);
        if (groups.stream().anyMatch(group -> group.getTopicId().equals(id))) {
            throw new IllegalArgumentException("Không thể xóa đề tài đã có nhóm đăng ký");
        }
        topics.removeIf(topic -> topic.getId().equals(id));
        save();
    }

    private void validateTopicDetails(String title, String description, String department) {
        if (title == null || title.isBlank() || description == null || description.isBlank()
                || !departments.contains(department)) {
            throw new IllegalArgumentException("Tên, nội dung và bộ môn là bắt buộc");
        }
    }

    public synchronized void publishTopic(Long id) {
        StudentTopic topic = findTopic(id);
        if (topic.getStatus() != TopicStatus.APPROVED) {
            throw new IllegalArgumentException("Đề tài phải được Trưởng khoa duyệt trước khi công bố");
        }
        topic.setStatus(TopicStatus.PUBLISHED);
        save(); // Trạng thái công bố cũng cần tồn tại sau khi ứng dụng khởi động lại.
    }

    public synchronized void approveTopic(Long id, UserAccount approver) {
        if (!isFacultyHead(approver)) {
            throw new IllegalArgumentException("Chỉ khoa và admin được duyệt đề tài");
        }
        StudentTopic topic = findTopic(id);
        if (topic.getStatus() != TopicStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ đề tài đang ở trạng thái bản nháp mới được duyệt");
        }
        topic.setStatus(TopicStatus.APPROVED);
        save();
    }

    private void createGroup(String name, String leader, String members, Long topicId) {
        List<String> studentIds = memberIds(members); // Chuẩn hóa danh sách trước khi áp dụng giới hạn.
        if (name == null || name.isBlank() || studentIds.size() < 3 || studentIds.size() > MAX_STUDENTS_PER_TOPIC) {
            throw new IllegalArgumentException("Mỗi nhóm phải có tên và từ 3 đến 5 sinh viên");
        }
        if (studentIds.stream().distinct().count() != studentIds.size()) {
            throw new IllegalArgumentException("Mã sinh viên không được lặp lại");
        }
        String leaderId = studentIdOf(leader);
        if (leaderId == null || studentIds.stream().filter(leaderId::equals).count() != 1) {
            throw new IllegalArgumentException("Nhóm phải có đúng một nhóm trưởng thuộc danh sách thành viên");
        }
        StudentTopic topic = findTopic(topicId);
        if (topic.getStatus() != TopicStatus.PUBLISHED) {
            throw new IllegalArgumentException("Đề tài chưa mở đăng ký");
        }
        if (registeredStudentCount(topicId) + studentIds.size() > MAX_STUDENTS_PER_TOPIC) {
            throw new IllegalArgumentException("Đề tài đã đủ tối đa 5 sinh viên đăng ký");
        }
        if (groups.stream().anyMatch(group -> findTopic(group.getTopicId()).getPeriodId().equals(topic.getPeriodId())
                && memberIds(group.getMembers()).stream().anyMatch(studentIds::contains))) {
            throw new IllegalArgumentException("Sinh viên đã thuộc một nhóm khác trong đợt này");
        }
        if (groups.stream().anyMatch(group -> group.getTopicId().equals(topicId))) {
            throw new IllegalArgumentException("Đề tài này đã có nhóm đăng ký"); // Mỗi đề tài chỉ nhận một nhóm.
        }
        RegistrationPeriod period = findPeriod(topic.getPeriodId());
        if (!period.isPublished() || LocalDate.now().isBefore(period.getStudentStart())
                || LocalDate.now().isAfter(period.getStudentEnd())) {
            throw new IllegalArgumentException("Đợt hiện chưa mở giai đoạn đăng ký dành cho sinh viên");
        }
        groups.add(new StudentGroup(ids.getAndIncrement(), name.trim(), leader.trim(), members.trim(), topicId));
        save(); // Tên nhóm, thành viên và đăng ký được lưu ngay.
    }

    // Bắt buộc người gửi yêu cầu phải có mặt trong nhóm; ngăn đăng ký thay sinh
    // viên khác.
    public synchronized void createGroup(String name, String leader, String members, Long topicId,
            UserAccount user) {
        if (user == null || user.getStudentId() == null
                || !memberIds(members).contains(user.getStudentId())) {
            throw new IllegalArgumentException("Danh sách thành viên phải có mã sinh viên đang đăng nhập");
        }
        createGroup(name, leader, members, topicId);
    }

    public synchronized void approveGroup(Long id, UserAccount approver) {
        if (!isFacultyHead(approver)) {
            throw new IllegalArgumentException("Chỉ khoa và admin được duyệt nhóm");
        }
        StudentGroup group = findGroup(id);
        if (!"Chờ duyệt".equals(group.getStatus())) {
            throw new IllegalArgumentException("Chỉ nhóm đang chờ duyệt mới được phê duyệt");
        }
        List<String> studentIds = memberIds(group.getMembers());
        if (studentIds.size() < 3 || studentIds.size() > MAX_STUDENTS_PER_TOPIC
                || studentIds.stream().distinct().count() != studentIds.size()
                || !studentIds.contains(studentIdOf(group.getLeader()))) {
            throw new IllegalArgumentException("Nhóm phải có từ 3 đến 5 thành viên duy nhất và đúng một nhóm trưởng");
        }
        group.setStatus("Đã duyệt");
        findTopic(group.getTopicId()).setStatus(TopicStatus.ASSIGNED);
        save(); // Lưu đồng thời trạng thái nhóm và đề tài.
    }

    private void submitReport(Long id) {
        StudentGroup group = findGroup(id);
        group.setReportSubmitted(true);
        group.setStatus("Chờ phản biện");
        save();
    }

    // Chỉ thành viên của nhóm mới có thể gửi báo cáo cho nhóm đó.
    public synchronized void submitReport(Long id, UserAccount user) {
        StudentGroup group = findGroup(id);
        if (!"Đã duyệt".equals(group.getStatus()) || group.isReportSubmitted()) {
            throw new IllegalArgumentException("Chỉ nhóm đã duyệt và chưa nộp báo cáo mới được nộp");
        }
        if (user == null || !memberIds(group.getMembers()).contains(user.getStudentId())
                || !studentIdOf(group.getLeader()).equals(user.getStudentId())) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được nộp báo cáo cho nhóm của mình");
        }
        submitReport(id);
    }

    public synchronized void grade(Long id, Double reviewerScore, Double councilScore, UserAccount grader) {
        // Chặn điểm âm, lớn hơn 10, null, NaN và vô cực ở tầng nghiệp vụ.
        if (!validScore(reviewerScore) || !validScore(councilScore)) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10");
        }
        StudentGroup group = findGroup(id);
        StudentTopic topic = findTopic(group.getTopicId());
        if (!group.isReportSubmitted()) {
            throw new IllegalArgumentException("Nhóm phải nộp báo cáo trước khi được chấm điểm");
        }
        if (!isFacultyHead(grader)) {
            throw new IllegalArgumentException("Chỉ khoa và admin được nhập điểm");
        }
        if (topic.getSupervisor().equalsIgnoreCase(grader.getDisplayName())) {
            throw new IllegalArgumentException("Giảng viên hướng dẫn không được chấm điểm đề tài của mình");
        }
        group.setReviewerScore(reviewerScore);
        group.setCouncilScore(councilScore);
        group.setStatus("Đã công bố kết quả");
        topic.setStatus(TopicStatus.COMPLETED);
        for (String studentId : memberIds(group.getMembers())) {
            addNotification(studentId, "Đã công bố điểm",
                    "Kết quả đề tài \"" + topic.getTitle() + "\" của nhóm " + group.getName() + " đã được công bố.");
        }
        save(); // Điểm và trạng thái công bố không mất khi khởi động lại.
    }

    private boolean isFacultyHead(UserAccount user) {
        return user != null && (user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.DEPARTMENT_HEAD);
    }

    private void addNotification(String recipientUsername, String title, String message) {
        notifications.add(new NotificationItem(ids.getAndIncrement(), recipientUsername, title, message));
    }

    private boolean validScore(Double score) {
        return score != null && Double.isFinite(score) && score >= 0 && score <= 10;
    }

    private List<String> memberIds(String members) {
        if (members == null || members.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(members.split(","))
                .map(member -> member == null ? "" : member.trim())
                .filter(member -> !member.isEmpty())
                .map(this::studentIdOf)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String studentIdOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.trim().split("\\s+-\\s+", 2);
        if (parts.length == 1) {
            return parts[0].trim();
        }
        return parts[0].trim().matches("[A-Za-z0-9._]+") ? parts[0].trim() : parts[1].trim();
    }

    private StudentTopic findTopic(Long id) {
        return topics.stream().filter(topic -> topic.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài"));
    }

    private RegistrationPeriod findPeriod(Long id) {
        return periods.stream().filter(period -> period.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));
    }

    private StudentGroup findGroup(Long id) {
        return groups.stream().filter(group -> group.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm"));
    }

    private void seedDemoData() {
        // Dữ liệu mẫu chỉ dùng khi chưa tồn tại tệp JSON lưu trữ.
        LocalDate today = LocalDate.now();
        RegistrationPeriod period = new RegistrationPeriod(1L, "KLTN học kỳ 1 - 2026", ProjectType.KLTN,
                today.minusDays(10), today.plusDays(8), today, today.plusDays(20), today.plusDays(45),
                today.plusDays(60));
        period.setPublished(true);
        periods.add(period);
        StudentTopic topic = new StudentTopic(1L, "Cổng thông tin đăng ký đề tài CNTT",
                "Xây dựng quy trình đăng ký, phê duyệt và công bố kết quả đề tài.",
                "Công nghệ phần mềm", "Hoàng Thái Xuân Khoa", "", 1L);
        topic.setStatus(TopicStatus.PUBLISHED);
        topics.add(topic);
        groups.add(new StudentGroup(2L, "SE-01", "Hoàng Hữu Nguyên Anh - 21161067",
                "21161067 - Hoàng Hữu Nguyên Anh, 20146298 - Hoàng Gia Bảo, 22146178 - Trần Hải Nam", 1L));
    }

    private void load() {
        try {
            PersistedData data = dataMapper.readValue(dataFile.toFile(), PersistedData.class);
            // Không tiếp tục âm thầm với cấu trúc hỏng vì như vậy có thể làm mất dữ liệu
            // hiện có.
            if (data.periods == null || data.topics == null || data.groups == null || data.nextId < 1) {
                throw new IllegalStateException("Tệp dữ liệu đề tài không hợp lệ: " + dataFile);
            }
            periods.clear();
            periods.addAll(data.periods);
            departments.clear();
            departments.addAll(data.departments == null ? DEFAULT_DEPARTMENTS : data.departments);
            topics.clear();
            topics.addAll(data.topics);
            groups.clear();
            groups.addAll(data.groups);
            notifications.clear();
            if (data.notifications != null) {
                notifications.addAll(data.notifications);
            }
            ids.set(data.nextId);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Không thể đọc dữ liệu đề tài từ " + dataFile, exception);
        }
    }

    private void save() {
        PersistedData data = new PersistedData();
        data.periods = periods;
        data.departments = departments;
        data.topics = topics;
        data.groups = groups;
        data.notifications = notifications;
        data.nextId = ids.get();
        // Ghi sang tệp tạm trước để tránh tệp chính bị ghi dở nếu tiến trình dừng giữa
        // chừng.
        Path temporaryFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(dataFile.getParent());
            dataMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), data);
            try {
                // Đổi tên nguyên tử khi hệ thống tệp hỗ trợ thao tác này.
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (JacksonException | IOException exception) {
            throw new IllegalStateException("Không thể lưu dữ liệu đề tài vào " + dataFile, exception);
        }
    }

    public static class PersistedData {
        // DTO lưu phiên bản dữ liệu đủ để khôi phục toàn bộ trạng thái và bộ cấp mã.
        public List<RegistrationPeriod> periods = new ArrayList<>();
        public List<String> departments = new ArrayList<>(DEFAULT_DEPARTMENTS);
        public List<StudentTopic> topics = new ArrayList<>();
        public List<StudentGroup> groups = new ArrayList<>();
        public List<NotificationItem> notifications = new ArrayList<>();
        public long nextId = 10;

        public PersistedData() {
        }
    }
}
