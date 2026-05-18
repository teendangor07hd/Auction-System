package com.bidhub.server.network;

import com.bidhub.common.exception.AuthenticationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.Optional;

/**
 * Utility kiem tra quyen truy cap — dung trong moi handler can auth/role guard.
 *
 * <p>Static method ném {@link AuthenticationException} neu khong dat dieu kien.
 * Exception nay duoc bat boi try-catch trong {@link RequestHandler#handle} va tra ve error response.
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — AuthenticationException cho auth guard]
 */
public final class SecurityContext {

    private static final UserDao USER_DAO = new UserDao();

    private SecurityContext() {}

    /**
     * Yeu cau nguoi dung da dang nhap — tra ve userId neu hop le.
     *
     * @param session session hien tai
     * @return userId cua nguoi dung da dang nhap
     * @throws AuthenticationException neu chua dang nhap
     */
    public static String requireAuthenticated(Session session) {
        if (session == null || !session.isAuthenticated()) {
            throw new AuthenticationException("Ban chua dang nhap. Vui long LOGIN truoc.");
        }
        return session.getAuthenticatedUserId();
    }

    /**
     * Yeu cau nguoi dung co dung role — tra ve userId neu hop le.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — kiem tra role truoc khi thuc hien hanh dong]
     *
     * @param session  session hien tai
     * @param required role yeu cau
     * @return userId cua nguoi dung co dung role
     * @throws AuthenticationException neu chua dang nhap hoac sai role
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