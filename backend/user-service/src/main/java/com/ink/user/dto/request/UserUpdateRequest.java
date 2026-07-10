package com.ink.user.dto.request;

import lombok.Data;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
}
