package com.bidhub.server.service;

import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.List;

/**
 * Dich vu quan tri: liet ke, khoa, mo khoa tai khoan nguoi dung.
 *
 * <p>Goi {@link UserDao} va {@link AuditLogService} de thuc hien thao tac admin.
 * Moi thao tac lock/unlock deu duoc ghi nhat ky qua audit log.
 *
 * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin lock/unlock tai khoan]
 */
public class  AdminUserService {

  private final UserDao userDao;
  private final AuditLogService auditLogService;

  /** Constructor production — tao UserDao va AuditLogService moi. */
  public AdminUserService() {
    this.userDao = new UserDao();
    this.auditLogService = new AuditLogService();
  }

  /**
   * Constructor cho test — inject UserDao va AuditLogService.
   *
   * @param userDao         UserDao inject
   * @param auditLogService AuditLogService inject
   */
  public AdminUserService(UserDao userDao, AuditLogService auditLogService) {
    this.userDao = userDao;
    this.auditLogService = auditLogService;
  }

  /**
   * Lay danh sach toan bo nguoi dung.
   *
   * @return danh sach user
   */
  public List<User> listAllUsers() {
    return userDao.findAll();
  }

  /**
   * Khoa tai khoan nguoi dung — chi Admin duoc goi.
   *
   * <p>Kiem tra: user ton tai, khong phai ADMIN → cap nhat is_locked → log.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin khoa tai khoan]
   *
   * @param targetId id cua nguoi dung bi khoa
   * @param adminId  id cua admin thuc hien
   * @throws UserNotFoundException neu target khong ton tai
   * @throws ValidationException   neu target la ADMIN
   */
  public void lockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    // 📌 [Tieu chi: Xu ly loi & ngoai le — khong khoa Admin]
    if (target.getRole() == UserRole.ADMIN) {
      throw new ValidationException("Khong the khoa tai khoan Admin.");
    }

    userDao.updateLocked(targetId, true);

    // 📌 [Tieu chi: Audit trail — ghi nhat ky khi khoa user]
    auditLogService.log(adminId, AuditActions.USER_LOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }

  /**
   * Mo khoa tai khoan nguoi dung.
   *
   * <p>Kiem tra user ton tai → cap nhat is_locked → log.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin mo khoa tai khoan]
   *
   * @param targetId id cua nguoi dung bi mo khoa
   * @param adminId  id cua admin thuc hien
   * @throws UserNotFoundException neu target khong ton tai
   */
  public void unlockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    userDao.updateLocked(targetId, false);

    // 📌 [Tieu chi: Audit trail — ghi nhat ky khi mo khoa user]
    auditLogService.log(adminId, AuditActions.USER_UNLOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }
}
