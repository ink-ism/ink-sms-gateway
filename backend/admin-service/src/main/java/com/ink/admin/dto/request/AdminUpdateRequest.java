package com.ink.admin.dto.request;

import lombok.Data;

/**
 * 管理员信息更新请求
 */
@Data
public class AdminUpdateRequest {

    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private String role;
}
