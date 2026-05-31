package com.bidhub.server.service;

import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.List;

/**
 * Dich vu quản trị: liet ke, khoa, mo khoa tài khoản người dùng.
 *
 * <p>Goi {@link UserDao} và {@link AuditLogService} để thực hiện thao tac admin.
 * Moi thao tac lock/unlock deu được ghi nhat ky qua audit log.
 *
 */
public class  AdminUserService {

  private final UserDao userDao;
  private final AuditLogService auditLogService;

  /** Constructor production — tạo UserDao và AuditLogService moi. */
  public AdminUserService() {
    this.userDao = new UserDao();
    this.auditLogService = new AuditLogService();
  }

  /**
   * Constructor cho test — inject UserDao và AuditLogService.
   *
   * @param userDao         UserDao inject
   * @param auditLogService AuditLogService inject
   */
  public AdminUserService(UserDao userDao, AuditLogService auditLogService) {
    this.userDao = userDao;
    this.auditLogService = auditLogService;
  }

  /**
   * Lấy danh sach toàn bộ người dùng.
   *
   * @return danh sach user
   */
  public List<User> listAllUsers() {
    return userDao.findAll();
  }

  /**
   * Khoa tài khoản người dùng — chỉ Admin được goi.
   *
   * <p>Kiem tra: user ton tai, không phai ADMIN → cập nhật is_locked → log.
   *
   *
   * @param targetId id cua người dùng bi khoa
   * @param adminId  id cua admin thực hiện
   * @throws UserNotFoundException nếu target không tồn tại
   * @throws ValidationException   nếu target là ADMIN
   */
  public void lockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    if (target.getRole() == UserRole.ADMIN) {
      throw new ValidationException("Khong the khoa tai khoan Admin.");
    }

    userDao.updateLocked(targetId, true);

    auditLogService.log(adminId, AuditActions.USER_LOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }

  /**
   * Mo khoa tài khoản người dùng.
   *
   * <p>Kiem tra user ton tai → cập nhật is_locked → log.
   *
   *
   * @param targetId id cua người dùng bi mo khoa
   * @param adminId  id cua admin thực hiện
   * @throws UserNotFoundException nếu target không tồn tại
   */
  public void unlockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    userDao.updateLocked(targetId, false);

    auditLogService.log(adminId, AuditActions.USER_UNLOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }
}
