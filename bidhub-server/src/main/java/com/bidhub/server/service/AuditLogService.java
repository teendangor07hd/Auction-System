package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dich vu ghi nhat ky audit — wrap AuditLogDao với try-catch, không bao giờ ném exception.
 *
 * Moi handler goi {@link #log(String, String, String)} để ghi nhat ky.
 * Nếu DAO loi → chỉ in stderr, không ném ra ngoai → business logic tiep tuc binh thuong.
 *
 * <p>2 constructor: production (tạo AuditLogDao moi) và test (inject AuditLogDao từ ngoai).
 */
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogDao auditLogDao;

    /**
     * Constructor production — tạo AuditLogDao từ DbConnectionProvider.
     */
    public AuditLogService() {
        this.auditLogDao = new AuditLogDao();
    }

    /**
     * Constructor test — inject AuditLogDao từ ngoai (vi du in-memory SQLite).
     *
     * @param auditLogDao DAO inject
     */
    public AuditLogService(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /**
     * Ghi 1 ban ghi audit log — không bao giờ ném exception ra ngoai.
     *
     * Nếu loi → in System.err, không lam phanh business logic chinh.
     * userId có the null (system action).
     *
     * @param userId  id người dùng (có the null cho system action)
     * @param action  ma hanh đóng từ AuditActions
     * @param details chỉ tiet thêm (JSON string, vi du "{}")
     */
    public void log(String userId, String action, String details) {
        try {
            AuditLog entry = new AuditLog(userId, action, details);
            auditLogDao.save(entry);
        } catch (Exception e) {
            logger.error("Khong the ghi log: action={}, userId={}, error={}",
                    action, userId, e.getMessage(), e);
        }
    }
}
