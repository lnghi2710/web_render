package com.example.meetingroom.controller;

import com.example.meetingroom.model.UserAccount;
import com.example.meetingroom.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController { // Xử lý đăng nhập, đăng xuất và phiên người dùng.
    public static final String SESSION_USER = "currentUser"; // Khóa lưu người dùng trong session.
    private final AuthService authService; // Dịch vụ xác thực tài khoản.

    public AuthController(AuthService authService) { // Nhận dịch vụ xác thực từ Spring.
        this.authService = authService; // Lưu dịch vụ để dùng trong các endpoint.
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout, Model model) {
        Object storedUser = session.getAttribute(SESSION_USER);
        if (storedUser instanceof UserAccount account && authService.activeAccount(account.getUsername()) != null) {
            return "redirect:/projects";
        }
        session.removeAttribute(SESSION_USER);
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không đúng");
        } else if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất an toàn");
        }
        return "login";
    }
}