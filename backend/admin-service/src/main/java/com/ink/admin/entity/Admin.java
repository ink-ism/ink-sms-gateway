package com.ink.admin.entity;

import com.ink.common.constant.AdminConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 管理员实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Admin {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private String role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    /**
     * 创建新管理员
     */
    public static Admin create(String username, String password, String email, String phone, String role) {
        return new Admin()
                .setUsername(username)
                .setPassword(password)
                .setEmail(email)
                .setPhone(phone)
                .setNickname(username)
                .setRole(role)
                .setStatus(AdminConstants.STATUS_NORMAL)
                .setCreateTime(LocalDateTime.now());
    }

    /**
     * 密码脱敏（将密码置空）
     */
    public Admin maskPassword() {
        this.password = null;
        return this;
    }
}
