package com.ink.admin.service;

import com.ink.common.exception.BusinessException;
import com.ink.common.utils.JwtUtil;
import com.ink.common.utils.RedisUtil;
import com.ink.admin.entity.Admin;
import com.ink.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /**
     * 创建管理员（需管理员登录）
     */
    @Transactional
    public void createAdmin(Admin admin) {
        if (adminMapper.existsByUsername(admin.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (adminMapper.existsByEmail(admin.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        if (adminMapper.existsByPhone(admin.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        if (adminMapper.insert(admin) <= 0) {
            throw new BusinessException("创建失败");
        }
        log.info("管理员创建成功: {}", admin.getUsername());
    }

    /**
     * 管理员登录
     */
    public Map<String, Object> login(String username, String password, String ip) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getId().toString(), admin.getUsername());
        adminMapper.updateLastLoginTime(admin.getId(), LocalDateTime.now(), ip);

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("email", admin.getEmail());
        adminInfo.put("nickname", admin.getNickname());
        adminInfo.put("avatar", admin.getAvatar());
        adminInfo.put("role", admin.getRole());
        adminInfo.put("loginTime", LocalDateTime.now().toString());

        redisUtil.setUserInfo(admin.getId().toString(), adminInfo);
        redisUtil.setUserToken(admin.getId().toString(), token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminInfo", adminInfo);

        log.info("管理员登录成功: {}", username);
        return result;
    }

    /**
     * 管理员登出
     */
    public void logout(String adminId) {
        redisUtil.removeUserToken(adminId);
        redisUtil.removeUserInfo(adminId);
        log.info("管理员登出成功: adminId={}", adminId);
    }

    /**
     * 根据ID获取管理员
     */
    public Admin getAdminById(Long id) {
        Admin admin = adminMapper.findById(id);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }

    /**
     * 根据用户名获取管理员
     */
    public Admin getAdminByUsername(String username) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }

    /**
     * 更新管理员信息
     */
    @Transactional
    public void updateAdmin(Admin admin) {
        Admin existingAdmin = adminMapper.findById(admin.getId());
        if (existingAdmin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!existingAdmin.getUsername().equals(admin.getUsername()) && adminMapper.existsByUsername(admin.getUsername())) {
            throw new BusinessException("用户名已被使用");
        }
        if (!existingAdmin.getEmail().equals(admin.getEmail()) && adminMapper.existsByEmail(admin.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }
        if (!existingAdmin.getPhone().equals(admin.getPhone()) && adminMapper.existsByPhone(admin.getPhone())) {
            throw new BusinessException("手机号已被使用");
        }

        admin.setUpdateTime(LocalDateTime.now());
        if (adminMapper.update(admin) <= 0) {
            throw new BusinessException("更新失败");
        }

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("email", admin.getEmail());
        adminInfo.put("nickname", admin.getNickname());
        adminInfo.put("avatar", admin.getAvatar());
        adminInfo.put("role", admin.getRole());
        redisUtil.setUserInfo(admin.getId().toString(), adminInfo);

        log.info("管理员信息更新成功: adminId={}", admin.getId());
    }

    /**
     * 修改密码
     */
    @Transactional
    public void updatePassword(Long adminId, String oldPassword, String newPassword) {
        Admin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdateTime(LocalDateTime.now());

        if (adminMapper.update(admin) <= 0) {
            throw new BusinessException("密码修改失败");
        }

        redisUtil.removeUserToken(adminId.toString());
        log.info("密码修改成功: adminId={}", adminId);
    }

    /**
     * 获取管理员列表
     */
    public List<Admin> getAdminList(int page, int size) {
        return adminMapper.findByPage((page - 1) * size, size);
    }

    /**
     * 获取管理员总数
     */
    public int getAdminCount() {
        return adminMapper.count();
    }

    /**
     * 禁用管理员
     */
    @Transactional
    public void disableAdmin(Long adminId) {
        if (adminMapper.findById(adminId) == null) {
            throw new BusinessException("管理员不存在");
        }
        adminMapper.updateStatus(adminId, 0);
        redisUtil.removeUserToken(adminId.toString());
        log.info("管理员禁用成功: adminId={}", adminId);
    }

    /**
     * 启用管理员
     */
    @Transactional
    public void enableAdmin(Long adminId) {
        if (adminMapper.findById(adminId) == null) {
            throw new BusinessException("管理员不存在");
        }
        adminMapper.updateStatus(adminId, 1);
        log.info("管理员启用成功: adminId={}", adminId);
    }

    /**
     * 重置密码（临时方法）
     */
    @Transactional
    public void resetPassword(String username, String newPassword) {
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.update(admin);
        log.info("密码重置成功: username={}", username);
    }
}
