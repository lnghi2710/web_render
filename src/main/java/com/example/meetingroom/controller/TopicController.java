package com.example.meetingroom.controller;

import com.example.meetingroom.model.ProjectType;
import com.example.meetingroom.model.UserAccount;
import com.example.meetingroom.model.UserRole;
import com.example.meetingroom.model.TopicStatus;
import jakarta.servlet.http.HttpSession;
import com.example.meetingroom.service.AuthService;
import com.example.meetingroom.service.TopicManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Controller điều phối request MVC; giao diện được render ở templates, còn quy tắc nghiệp vụ nằm trong service.
@Controller
@RequestMapping("/projects")
public class TopicController { // Nhận yêu cầu dashboard và các thao tác đề tài.
    private final TopicManagementService service; // Dịch vụ xử lý nghiệp vụ đề tài.
    private final AuthService authService;

    public TopicController(TopicManagementService service, AuthService authService) { // Nhận service do Spring quản lý.
        this.service = service; // Lưu service để dùng trong endpoint.
        this.authService = authService;
    }

    @GetMapping // Mở dashboard theo phiên đăng nhập.
    public String dashboard(Model model, HttpSession session,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) TopicStatus status,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) String supervisor) {
        UserAccount user = currentUser(session); // Lấy người dùng hiện tại từ session.
        if (user == null)
            return "redirect:/login"; // Yêu cầu đăng nhập trước khi xem dashboard.
        model.addAttribute("currentUser", user); // Cung cấp tên và vai trò cho giao diện.
        model.addAttribute("canManage", user.isAdmin() || user.getRole() == UserRole.DEPARTMENT_HEAD
                || user.canSupervise());
        model.addAttribute("canAdmin", user.isAdmin()); // Chỉ ban chủ nhiệm khoa được chấm điểm và điều phối đợt.
        model.addAttribute("canFacultyHead", user.isAdmin() || user.getRole() == UserRole.DEPARTMENT_HEAD);
        model.addAttribute("canGrade", user.isAdmin() || user.getRole() == UserRole.DEPARTMENT_HEAD);
        model.addAttribute("canCreateTopic", user.canSupervise());
        model.addAttribute("canSupervise", user.canSupervise());
        model.addAttribute("periods", service.periods()); // Nạp danh sách đợt đăng ký.
        var topics = service.searchTopics(keyword, department, status, periodId, supervisor);
        model.addAttribute("topics", topics); // Nạp danh sách đề tài.
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("supervisorFilter", supervisor);
        model.addAttribute("registeredCounts", topics.stream().collect(Collectors.toMap(
                topic -> topic.getId(), topic -> service.registeredStudentCount(topic.getId()))));
        model.addAttribute("availableTopics", service.availableTopics()); // Chỉ đề tài còn khả năng nhận đăng ký.
        model.addAttribute("groups", service.groupsFor(user, periodId)); // Lọc nhóm và kết quả theo đợt được chọn.
        model.addAttribute("notifications", service.notificationsFor(user));
        model.addAttribute("unreadNotificationCount", service.notificationsFor(user).stream()
                .filter(notification -> !notification.isRead()).count());
        model.addAttribute("types", ProjectType.values()); // Cung cấp các loại đề tài cho form.
        model.addAttribute("departments", service.departments()); // Danh mục cho form chọn bộ môn.
        model.addAttribute("accounts", user.isAdmin() ? authService.accounts() : java.util.List.of());
        model.addAttribute("canSendNotifications", user.isTeacher());
        model.addAttribute("notificationRecipients", user.isTeacher() ? authService.accounts() : java.util.List.of());
        model.addAttribute("teachers", authService.teachers());
        return "projects/dashboard"; // Render dashboard.
    }

    @PostMapping("/notifications/read")
    @PreAuthorize("isAuthenticated()")
    public String markNotificationRead(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        return run(() -> service.markNotificationRead(id, currentUser(session)), redirect);
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD', 'TEACHER', 'SUPERVISOR', 'REVIEWER', 'COUNCIL')")
    public String createAnnouncement(@RequestParam String title, @RequestParam String message,
            @RequestParam(required = false) java.util.List<String> recipients,
            RedirectAttributes redirect, HttpSession session) {
        return run(() -> {
            UserAccount sender = currentUser(session);
            if (recipients == null
                    || recipients.stream().anyMatch(username -> authService.activeAccount(username) == null)) {
                throw new IllegalArgumentException("Vui lòng chọn người nhận hợp lệ");
            }
            service.createAnnouncement(title, message, recipients, sender);
        }, redirect);
    }

    @PostMapping("/announcements/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAnnouncement(@RequestParam Long id, RedirectAttributes redirect) {
        return run(() -> service.deleteAnnouncement(id), redirect);
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public String createDepartment(@RequestParam String name, RedirectAttributes redirect) {
        return run(() -> service.createDepartment(name), redirect);
    }

    @PostMapping("/departments/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateDepartment(@RequestParam String currentName, @RequestParam String name,
            RedirectAttributes redirect) {
        return run(() -> service.updateDepartment(currentName, name), redirect);
    }

    @PostMapping("/departments/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDepartment(@RequestParam String name, RedirectAttributes redirect) {
        return run(() -> service.deleteDepartment(name), redirect);
    }

    @GetMapping("/reports/topics.xlsx")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTopics(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department, @RequestParam(required = false) TopicStatus status,
            @RequestParam(required = false) Long periodId, @RequestParam(required = false) String supervisor)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Đề tài");
            Row header = sheet.createRow(0);
            String[] columns = { "Mã", "Đề tài", "Bộ môn", "Giảng viên hướng dẫn", "Đợt", "Trạng thái", "Số SV" };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowNumber = 1;
            for (var topic : service.searchTopics(keyword, department, status, periodId, supervisor)) {
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(topic.getId());
                row.createCell(1).setCellValue(topic.getTitle());
                row.createCell(2).setCellValue(topic.getDepartment());
                row.createCell(3).setCellValue(topic.getSupervisor());
                row.createCell(4).setCellValue(String.valueOf(topic.getPeriodId()));
                row.createCell(5).setCellValue(topic.getStatus().getLabel());
                row.createCell(6).setCellValue(service.registeredStudentCount(topic.getId()));
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"topics.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(output.toByteArray());
        }
    }

    @PostMapping("/periods") // Nhận form tạo đợt đăng ký.
    @PreAuthorize("hasRole('ADMIN')")
    public String createPeriod(@RequestParam String name, @RequestParam ProjectType type,
            @RequestParam LocalDate teacherStart, @RequestParam LocalDate teacherEnd,
            @RequestParam LocalDate studentStart, @RequestParam LocalDate studentEnd,
            @RequestParam(required = false) LocalDate reviewerDeadline,
            @RequestParam(required = false) LocalDate defenseDate, RedirectAttributes redirect, HttpSession session) {
        if (!isAdmin(session))
            return forbidden(redirect); // Chỉ ban chủ nhiệm khoa được tạo đợt đăng ký.
        return run(() -> service.createPeriod(name, type, teacherStart, teacherEnd, studentStart, studentEnd,
                reviewerDeadline, defenseDate), redirect); // Gọi nghiệp vụ tạo và kiểm tra đợt.
    }

    // Các endpoint quản lý đợt và tài khoản đều xác thực quyền khoa phía máy chủ.
    @PostMapping("/periods/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePeriod(@RequestParam Long id, @RequestParam String name, @RequestParam ProjectType type,
            @RequestParam LocalDate teacherStart, @RequestParam LocalDate teacherEnd,
            @RequestParam LocalDate studentStart, @RequestParam LocalDate studentEnd,
            @RequestParam(required = false) LocalDate reviewerDeadline,
            @RequestParam(required = false) LocalDate defenseDate, RedirectAttributes redirect, HttpSession session) {
        if (!isAdmin(session))
            return forbidden(redirect);
        return run(() -> service.updatePeriod(id, name, type, teacherStart, teacherEnd, studentStart, studentEnd,
                reviewerDeadline, defenseDate), redirect);
    }

    @PostMapping("/periods/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePeriod(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isAdmin(session))
            return forbidden(redirect);
        return run(() -> service.deletePeriod(id), redirect);
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public String createAccount(@RequestParam String username, @RequestParam String password,
            @RequestParam String displayName, @RequestParam(required = false) String studentId,
            @RequestParam UserRole role, RedirectAttributes redirect, HttpSession session) {
        if (!isAdmin(session))
            return forbidden(redirect);
        return run(() -> authService.createAccount(username, password, displayName, studentId, role), redirect);
    }

    @PostMapping("/accounts/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAccount(@RequestParam String username, RedirectAttributes redirect, HttpSession session) {
        if (!isAdmin(session))
            return forbidden(redirect);
        return run(() -> authService.deleteAccount(username), redirect);
    }

    @PostMapping("/topics") // Nhận form tạo đề tài.
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPERVISOR')")
    public String createTopic(@RequestParam String title, @RequestParam String description,
            @RequestParam String department, @RequestParam Long periodId,
            RedirectAttributes redirect, HttpSession session) {
        if (!isSupervisor(session))
            return forbidden(redirect); // Chỉ giáo viên được tạo đề tài.
        UserAccount user = currentUser(session); // Lấy GV từ phiên, không tin tên GV do trình duyệt gửi lên.
        return run(() -> service.createTopic(title, description, department, user, periodId),
                redirect); // Lưu đề tài và trả thông báo kết quả.
    }

    @PostMapping("/topics/update") // Cập nhật nội dung nhưng giữ nguyên người hướng dẫn đã ghi nhận.
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPERVISOR')")
    public String updateTopic(@RequestParam Long id, @RequestParam String title,
            @RequestParam String description, @RequestParam String department,
            RedirectAttributes redirect, HttpSession session) {
        UserAccount user = currentUser(session);
        if (!isSupervisor(session))
            return forbidden(redirect);
        return run(() -> service.updateTopic(id, title, description, department, user), redirect);
    }

    // Form khoa chọn người hướng dẫn bằng username; tên hiển thị luôn lấy từ hồ sơ
    // tài khoản.
    @PostMapping("/topics/admin-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String adminUpdateTopic(@RequestParam Long id, @RequestParam String title,
            @RequestParam String description, @RequestParam String department,
            @RequestParam String supervisorUsername, @RequestParam Long periodId,
            RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect);
        UserAccount supervisor = authService.activeAccount(supervisorUsername);
        return run(() -> service.updateTopicAsAdmin(id, title, description, department, supervisor, periodId),
                redirect);
    }

    @PostMapping("/topics/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String deleteTopic(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect);
        return run(() -> service.deleteTopic(id), redirect);
    }

    @PostMapping("/topics/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String approveTopic(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect);
        return run(() -> service.approveTopic(id, currentUser(session)), redirect);
    }

    @PostMapping("/topics/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String publish(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect);
        return run(() -> service.publishTopic(id), redirect);
    }

    @PostMapping("/groups") // Nhận form đăng ký nhóm của sinh viên.
    @PreAuthorize("hasRole('STUDENT')")
    public String createGroup(@RequestParam String name, @RequestParam String leader,
            @RequestParam String members, @RequestParam Long topicId, RedirectAttributes redirect,
            HttpSession session) {
        if (!isStudent(session))
            return forbidden(redirect); // Chỉ sinh viên được đăng ký nhóm.
        UserAccount user = currentUser(session); // Lấy MSSV của tài khoản hiện tại.
        return run(() -> service.createGroup(name, leader, members, topicId, user), redirect); // Tạo nhóm gắn với MSSV
                                                                                               // đăng nhập.
    }

    @PostMapping("/groups/approve") // Nhận yêu cầu duyệt nhóm.
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String approve(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect); // Chỉ khoa và admin được duyệt nhóm.
        return run(() -> service.approveGroup(id, currentUser(session)), redirect); // Duyệt nhóm và phân công đề tài.
    }

    @PostMapping("/groups/report") // Nhận yêu cầu nộp báo cáo.
    @PreAuthorize("hasRole('STUDENT')")
    public String report(@RequestParam Long id, RedirectAttributes redirect, HttpSession session) {
        if (!isStudent(session))
            return forbidden(redirect); // Chỉ sinh viên được nộp báo cáo.
        UserAccount user = currentUser(session); // Lấy tài khoản để kiểm tra thành viên nhóm.
        return run(() -> service.submitReport(id, user), redirect); // Ghi nhận báo cáo của nhóm sinh viên.
    }

    @PostMapping("/groups/grade") // Nhận form nhập điểm và công bố.
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public String grade(@RequestParam Long id, @RequestParam Double reviewerScore,
            @RequestParam Double councilScore, RedirectAttributes redirect, HttpSession session) {
        if (!isFacultyHead(session))
            return forbidden(redirect); // Chỉ khoa và admin được nhập điểm.
        return run(() -> service.grade(id, reviewerScore, councilScore, currentUser(session)), redirect);
    }

    // Dùng flash message theo mẫu POST/Redirect/GET; lỗi nghiệp vụ được thông báo
    // thay vì bị bỏ qua.
    private String run(Runnable action, RedirectAttributes redirect) {
        try {
            action.run(); // Thực hiện nghiệp vụ được yêu cầu.
            redirect.addFlashAttribute("successMessage", "Thao tác đã được ghi nhận"); // Báo thao tác thành công.
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage()); // Hiện lỗi kiểm tra nghiệp vụ.
        }
        return "redirect:/projects"; // Quay lại dashboard.
    }

    private UserAccount currentUser(HttpSession session) { // Đọc tài khoản đã lưu trong phiên.
        Object user = session.getAttribute(AuthController.SESSION_USER); // Lấy giá trị session.
        return user instanceof UserAccount account ? authService.activeAccount(account.getUsername()) : null;
    }

    private boolean isStudent(HttpSession session) { // Kiểm tra phiên có quyền sinh viên.
        UserAccount user = currentUser(session); // Lấy tài khoản hiện tại.
        return user != null && user.isStudent(); // Chỉ trả true cho sinh viên đã đăng nhập.
    }

    // Tách quyền khoa khỏi quyền giáo viên để việc ẩn form không phải là lớp bảo vệ
    // duy nhất.
    private boolean isAdmin(HttpSession session) {
        UserAccount user = currentUser(session);
        return user != null && user.isAdmin();
    }

    private boolean isFacultyHead(HttpSession session) {
        UserAccount user = currentUser(session);
        return user != null && (user.isAdmin() || user.getRole() == UserRole.DEPARTMENT_HEAD);
    }

    private boolean isSupervisor(HttpSession session) {
        UserAccount user = currentUser(session);
        return user != null && user.canSupervise();
    }

    private String forbidden(RedirectAttributes redirect) { // Từ chối thao tác sai vai trò.
        redirect.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này"); // Hiện thông báo không
                                                                                                 // đủ quyền.
        return "redirect:/projects"; // Quay về dashboard.
    }
}