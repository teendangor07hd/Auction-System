package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;

/**
 * Bản ghi audit — lịch sử hành động người dùng và hệ thống.
 *
 * <p>Immutable sau khi tạo: không có setter. Không có {@code updatedAt} riêng.
 * Constructor DB-load dùng {@code createdAt} cho cả 2 tham số của Entity.
 */
public final class AuditLog extends Entity {

    private final String userId;  // null nếu là system action
    private final String action;  // mã từ AuditActions
    private final String details; // JSON string context, tối thiểu là "{}"

    /**
     * Tạo bản ghi audit mới — tự sinh id và createdAt qua Entity.
     *
     * @param userId  id người thực hiện, null nếu system action
     * @param action  mã từ {@link AuditActions}
     * @param details JSON string context
     */
    public AuditLog(String userId, String action, String details) {
        super(); // Entity() → tự sinh UUID + createdAt = now
        this.userId = userId;
        this.action = action != null ? action : "";
        this.details = details != null ? details : "{}";
    }

    /**
     * Constructor load từ DB — giữ nguyên id và createdAt gốc.
     *
     * <p>Bảng audit_logs không có updated_at → truyền createdAt cho cả 2 tham số Entity.
     *
     * @param id        id từ DB
     * @param createdAt thời điểm tạo từ DB
     * @param userId    có thể null (system action)
     * @param action    mã hành động
     * @param details   JSON string
     */
    public AuditLog(String id, LocalDateTime createdAt,
                    String userId, String action, String details) {
        super(id, createdAt, createdAt); // dùng createdAt cho updatedAt — audit log không thay đổi
        this.userId = userId;
        this.action = action != null ? action : "";
        this.details = details != null ? details : "{}";
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}