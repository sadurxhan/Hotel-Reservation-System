package com.loft.hotel.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminDashboardPageController {

    @Value("${app.admin-username:admin}")
    private String adminUsername;

    @Value("${app.admin-key:1234}")
    private String adminKey;

    @GetMapping("/admin/login")
    public String loginPage(HttpSession session) {
        if (session != null && Boolean.TRUE.equals(session.getAttribute(AdminApiController.SESSION_ADMIN_KEY))) {
            return "redirect:/admin";
        }
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String processLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session) {
        if (adminUsername.equalsIgnoreCase(username.trim()) && adminKey.equals(password.trim())) {
            if (session != null) {
                session.setAttribute(AdminApiController.SESSION_ADMIN_KEY, true);
            }
            return "redirect:/admin";
        }
        return "redirect:/admin/login?error=1";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(AdminApiController.SESSION_ADMIN_KEY);
            session.invalidate();
        }
        return "redirect:/admin/login?logout=1";
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String adminDashboard(
            @RequestParam(value = "tab", required = false, defaultValue = "overview") String tab,
            HttpSession session,
            Model model) {
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(AdminApiController.SESSION_ADMIN_KEY))) {
            return "redirect:/admin/login";
        }
        model.addAttribute("defaultTab", tab);
        model.addAttribute("adminKey", adminKey);
        return "admin/dashboard";
    }
}
