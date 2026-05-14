package com.bidhub.server.model;

/**
 * Hằng số mã hành động audit — tập trung một chỗ, tránh hardcode string rải rác trong handlers.
 *
 * <p>Interface constant thay vì enum: action code là String thô để lưu thẳng vào DB
 * và dùng trong WHERE clause SQL mà không cần valueOf() conversion.
 */
public interface AuditActions {

    String USER_LOGIN      = "USER_LOGIN";
    String USER_LOGOUT     = "USER_LOGOUT";
    String USER_REGISTER   = "USER_REGISTER";
    String USER_LOCKED     = "USER_LOCKED";
    String USER_UNLOCKED   = "USER_UNLOCKED";
    String PLACE_BID       = "PLACE_BID";
    String AUCTION_CLOSED  = "AUCTION_CLOSED";
    String AUCTION_EXTENDED = "AUCTION_EXTENDED";
    String ITEM_CREATED    = "ITEM_CREATED";
    String ITEM_DELETED    = "ITEM_DELETED";
    String AUCTION_CREATED = "AUCTION_CREATED";
}