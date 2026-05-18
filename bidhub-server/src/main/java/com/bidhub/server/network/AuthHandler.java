package com.bidhub.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class AuthHandler {
    private final RequestHandler handler;

    AuthHandler(RequestHandler handler) {
        this.handler = handler;
    }

    String handleLogin(Session session, JsonNode payload) {
        String username = RequestHandler.getTextSafe(payload, "username");
        String password = RequestHandler.getTextSafe(payload, "password");

        if (username == null || username.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap khong duoc de trong."));
        }
        if (password == null || password.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Mat khau khong duoc de trong."));
        }

        Optional<User> userOpt = handler.userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
        }

        User user = userOpt.get();
        if (!AuthService.verifyPassword(password, user.getPasswordHash())) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
        }
        if (user.isLocked()) {
            handler.auditLogService.log(user.getId(),
                    AuditActions.USER_LOGIN, "{\"blocked\":true}");
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "TAI KHOAN BI KHOA"));
        }
        String token = SessionManager.getInstance().createSession(user.getId());
        session.setAuthenticatedUserId(user.getId());
        session.setUserRole(user.getRole()); // cache user role in session

        handler.auditLogService.log(user.getId(), AuditActions.USER_LOGIN, "{}");

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole().name());

        return MessageMapper.toJson(MessageResponse.ok("LOGIN", result));
    }

    String handleRegister(Session session, JsonNode payload) {
        String username = RequestHandler.getTextSafe(payload, "username");
        String password = RequestHandler.getTextSafe(payload, "password");
        String email = RequestHandler.getTextSafe(payload, "email");
        String roleStr = RequestHandler.getTextSafe(payload, "role");

        if (username == null || username.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Ten dang nhap khong duoc de trong."));
        }
        if (password == null || password.length() < 8) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Mat khau phai co it nhat 8 ky tu."));
        }
        if (email == null || !email.contains("@")) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Email khong hop le (phai chua @)."));
        }
        if (roleStr == null || "ADMIN".equalsIgnoreCase(roleStr)) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Khong the dang ky voi vai tro ADMIN."));
        }

        if (handler.userDao.existsByUsername(username)) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Ten dang nhap da ton tai."));
        }

        UserRole role = "SELLER".equalsIgnoreCase(roleStr)
                ? UserRole.SELLER : UserRole.BIDDER;
        String hashedPassword = AuthService.hashPassword(password);

        User newUser;
        if (role == UserRole.SELLER) {
            newUser = new Seller(username, hashedPassword, email);
        } else {
            newUser = new Bidder(username, hashedPassword, email);
        }

        handler.userDao.save(newUser);

        handler.auditLogService.log(newUser.getId(), AuditActions.USER_REGISTER, "{}");

        Map<String, String> result = new HashMap<>();
        result.put("userId", newUser.getId());
        result.put("username", newUser.getUsername());
        result.put("role", newUser.getRole().name());

        return MessageMapper.toJson(MessageResponse.ok("REGISTER", result));
    }

    String handleLogout(Session session, String token) {
        String userId = session.getAuthenticatedUserId();

        if (userId != null) {
            handler.auditLogService.log(userId, AuditActions.USER_LOGOUT, "{}");
        }

        if (token != null && !token.isBlank()) {
            SessionManager.getInstance().invalidateSession(token);
        }
        session.setAuthenticatedUserId(null);
        session.setUserRole(null);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Dang xuat thanh cong.");
        return MessageMapper.toJson(MessageResponse.ok("LOGOUT", result));
    }
}
