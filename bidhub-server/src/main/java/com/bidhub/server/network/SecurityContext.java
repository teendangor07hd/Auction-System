package com.bidhub.server.network;

import com.bidhub.common.exception.AuthenticationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.Optional;

/**
 * Utility kiem tra quyen truy cap — đúng trong moi handler cần auth/role guard.
 *
 * <p>Static method ném {@link AuthenticationException} nếu không dat điều kiện.
 * Exception này được bat boi try-catch trong {@link RequestHandler#handle} và trả về error response.
 *
 */
public final class SecurityContext {

    private static final UserDao USER_DAO = new UserDao();

    private SecurityContext() {}

    /**
     * Yeu cau người dùng đã đăng nhập — trả về userId nếu hop le.
     *
     * @param session session hien tai
     * @return userId cua người dùng đã đăng nhập
     * @throws AuthenticationException nếu chua đăng nhập
     */
    public static String requireAuthenticated(Session session) {
        if (session == null || !session.isAuthenticated()) {
            throw new AuthenticationException("Ban chua dang nhap. Vui long LOGIN truoc.");
        }
        return session.getAuthenticatedUserId();
    }

    /**
     * Yeu cau người dùng có đúng role — trả về userId nếu hop le.
     *
     *
     * @param session  session hien tai
     * @param required role yeu cau
     * @return userId cua người dùng có đúng role
     * @throws AuthenticationException nếu chua đăng nhập hoac sai role
     */
    public static String requireRole(Session session, UserRole required) {
        String userId = requireAuthenticated(session);

        if (session.getUserRole() == null) {
            Optional<User> userOpt = USER_DAO.findById(userId);
            if (userOpt.isEmpty()) {
                throw new AuthenticationException("Nguoi dung khong ton tai.");
            }
            session.setUserRole(userOpt.get().getRole());
        }

        if (session.getUserRole() != required) {
            throw new AuthenticationException(
                    "Khong du quyen. Yeu cau role: " + required.getDisplayName());
        }

        return userId;
    }
}