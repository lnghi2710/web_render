package com.example.meetingroom;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController { // Điều hướng các URL gốc của ứng dụng.

    @GetMapping("/") // Bắt yêu cầu truy cập trang chủ.
    public String index() { // Chuyển người dùng đến đăng nhập.
        return "redirect:/login"; // Hiển thị trang đăng nhập.
    }
}