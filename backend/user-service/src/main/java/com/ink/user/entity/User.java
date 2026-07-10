package com.ink.user.entity;

import com.ink.common.constant.UserConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    /**
     * 创建新用户
     */
    public static User create(String username, String password, String email, String phone) {
        return new User()
                .setUsername(username)
                .setPassword(password)
                .setEmail(email)
                .setPhone(phone)
                .setNickname(username)
                .setStatus(UserConstants.STATUS_NORMAL)
                .setCreateTime(LocalDateTime.now());
    }

    /**
     * 密码脱敏（将密码置空）
     */
    public User maskPassword() {
        this.password = null;
        return this;
    }
}
