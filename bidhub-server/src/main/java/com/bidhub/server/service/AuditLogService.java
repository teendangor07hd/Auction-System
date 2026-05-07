package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditLog;

/**
 * Dich vu ghi nhat ky audit — wrap AuditLogDao voi try-catch, khong bao gio nem exception.
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — audit log khong bao gio crash handler]
 * Moi handler goi {@link #log(String, String, String)} de ghi nhat ky.
 * Neu DAO loi → chi in stderr, khong nem ra ngoai → business logic tiep tuc binh thuong.
 *
 * <p>2 constructor: production (tao AuditLogDao moi) va test (inject AuditLogDao tu ngoai).
 */
public class AuditLogService {

    private final AuditLogDao auditLogDao;

    /**
     * Constructor production — tao AuditLogDao tu DbConnectionProvider.
     */
    public AuditLogService() {
        this.auditLogDao = new AuditLogDao();
    }

    /**
     * Constructor test — inject AuditLogDao tu ngoai (vi du in-memory SQLite).
     *
     * @param auditLogDao DAO inject
     */
    public AuditLogService(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /**
     * Ghi 1 ban ghi audit log — khong bao gio nem exception ra ngoai.
     *
     * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — wrap try-catch, khong nem exception]
     * Neu loi → in System.err, khong lam phanh business logic chinh.
     * userId co the null (system action).
     *
     * @param userId  id nguoi dung (co the null cho system action)
     * @param action  ma hanh dong tu AuditActions
     * @param details chi tiet them (JSON string, vi du "{}")
     */
    public void log(String userId, String action, String details) {
        try {
            AuditLog entry = new AuditLog(userId, action, details);
            auditLogDao.save(entry);
        } catch (Exception e) {
            // 📌 [Tieu chi: Xu ly loi — audit log khong duoc lam phanh handler]
            System.err.println("[AuditLogService] Khong the ghi log: "
                    + "action=" + action + ", userId=" + userId
                    + ", error=" + e.getMessage());
        }
    }
}

