package com.bidhub.common.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * POJO cho response từ server → client.
 *
 * <p>OK: {@code {"status":"OK","type":"PING","payload":{...}}}
 * ERROR: {@code {"status":"ERROR","type":"LOGIN","message":"Sai mật khẩu"}}
 * {@code @JsonInclude.NON_NULL} loại bỏ field null khỏi JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)   // ← thêm dòng này

public class MessageResponse {
    
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    private String status;
    private String type;
    private Object payload;
    private String message;

    /** No-arg constructor cho Jackson. */
    public MessageResponse() {}

    /**
     * Response thành công với payload bất kỳ.
     *
     * @param type    type tương ứng với request
     * @param payload object được serialize thành JSON
     */
    public static MessageResponse ok(String type, Object payload) {
        MessageResponse r = new MessageResponse();
        r.status = STATUS_OK;
        r.type = type;
        r.payload = payload;
        return r;
    }

    /**
     * Response lỗi với message tiếng Việt hiển thị cho client.
     *
     * @param type    type request gây lỗi
     * @param message thông báo lỗi
     */
    public static MessageResponse error(String type, String message) {
        MessageResponse r = new MessageResponse();
        r.status = STATUS_ERROR;
        r.type = type;
        r.message = message;
        return r;
    }

    public boolean isOk() {
        return STATUS_OK.equals(status);
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public String getMessage() {
        return message;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}