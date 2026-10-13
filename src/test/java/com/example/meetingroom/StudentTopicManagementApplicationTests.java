package com.example.meetingroom;

import com.example.meetingroom.model.UserAccount;
import com.example.meetingroom.service.AuthService;
import com.example.meetingroom.service.TopicManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class StudentTopicManagementApplicationTests { // Kiểm tra khởi động và dữ liệu demo.
    @Autowired
    private AuthService authService; // Service dùng để kiểm tra đăng nhập.

    @Autowired
    private TopicManagementService topicManagementService; // Service dùng kiểm tra quyền xem nhóm.
    @Autowired
    private MockMvc mockMvc;

    @Test // Đảm bảo toàn bộ Spring context có thể khởi động.
    void contextLoads() { // Test đạt khi context không ném lỗi.
    }

    @Test // Đảm bảo MSSV và mật khẩu demo đăng nhập đúng.
    void requestedStudentIdsCanLogIn() { // Kiểm tra ba tài khoản được cung cấp.
        assertEquals("Ban chủ nhiệm khoa", authService.authenticate("admin.khoa", "123456").getDisplayName());
        assertEquals(com.example.meetingroom.model.UserRole.ADMIN,
                authService.authenticate("admin.khoa", "123456").getRole());
        assertEquals("Hoàng Hữu Nguyên Anh", authService.authenticate("21161067", "123456").getDisplayName()); // Xác
                                                                                                               // nhận
                                                                                                               // tài
                                                                                                               // khoản
                                                                                                               // 21161067.
        assertEquals("Hoàng Gia Bảo", authService.authenticate("20146298", "123456").getDisplayName()); // Xác nhận tài
                                                                                                        // khoản
                                                                                                        // 20146298.
        assertEquals("Trần Hải Nam", authService.authenticate("22146178", "123456").getDisplayName()); // Xác nhận tài
                                                                                                       // khoản
                                                                                                       // 22146178.
        assertNull(authService.authenticate("21161067", "wrong-password")); // Từ chối sai mật khẩu.
        assertFalse(authService.authenticate("gv.khoa", "123456").isAdmin());
        assertTrue(authService.authenticate("admin.khoa", "123456").getPasswordHash().startsWith("$2"));
        assertNull(authService.activeAccount("gv.huongdan"));
        assertEquals("Giảng viên hướng dẫn", authService.authenticate("gv.hd", "123456").getDisplayName());
        assertEquals(com.example.meetingroom.model.UserRole.SUPERVISOR,
                authService.authenticate("gv.hd", "123456").getRole());
    }

    @Test // Đảm bảo mỗi sinh viên chỉ thấy nhóm có MSSV của mình.
    void eachRequestedStudentCanSeeTheSampleGroup() { // Kiểm tra nhóm mẫu có đủ ba thành viên.
        for (String studentId : new String[] { "21161067", "20146298", "22146178" }) { // Lặp qua các MSSV demo.
            UserAccount student = authService.authenticate(studentId, "123456"); // Đăng nhập sinh viên hiện tại.
            assertNotNull(student); // Xác nhận tài khoản tồn tại.
            assertTrue(topicManagementService.groupsFor(student).stream()
                    .anyMatch(group -> group.getMembers().contains(studentId))); // Xác nhận sinh viên thấy nhóm mình.
        }
    }

    @Test
    void loginRequiresCredentialsAndLogoutInvalidatesSession() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        var loginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "21161067")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/projects").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/projects/reports/topics.xlsx").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/projects/accounts/delete").with(csrf()).session(session)
                        .param("username", "gv.khoa"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void topicExcelExportIsAvailableToFacultyButNotStudents() throws Exception {
        var adminLogin = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin.khoa")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        var export = mockMvc.perform(get("/projects/reports/topics.xlsx").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        byte[] file = export.getResponse().getContentAsByteArray();
        assertTrue(file.length > 4);
        assertEquals((byte) 'P', file[0]);
        assertEquals((byte) 'K', file[1]);
    }
}