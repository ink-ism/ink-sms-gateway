package com.ink.user.service;

import com.ink.common.exception.BusinessException;
import com.ink.common.utils.JwtUtil;
import com.ink.common.utils.RedisUtil;
import com.ink.user.entity.User;
import com.ink.user.mapper.UserMapper;
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
 * 用户服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /**
     * 用户注册
     */
    @Transactional
    public void register(User user) {
        if (userMapper.existsByUsername(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.existsByEmail(user.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        if (userMapper.existsByPhone(user.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败");
        }
        log.info("用户注册成功: {}", user.getUsername());
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password, String ip) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId().toString(), user.getUsername());
        userMapper.updateLastLoginTime(user.getId(), LocalDateTime.now(), ip);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("loginTime", LocalDateTime.now().toString());

        redisUtil.setUserInfo(user.getId().toString(), userInfo);
        redisUtil.setUserToken(user.getId().toString(), token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        log.info("用户登录成功: {}", username);
        return result;
    }

    /**
     * 用户登出
     */
    public void logout(String userId) {
        redisUtil.removeUserToken(userId);
        redisUtil.removeUserInfo(userId);
        log.info("用户登出成功: userId={}", userId);
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public void updateUser(User user) {
        User existingUser = userMapper.findById(user.getId());
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        if (!existingUser.getUsername().equals(user.getUsername()) && userMapper.existsByUsername(user.getUsername())) {
            throw new BusinessException("用户名已被使用");
        }
        if (!existingUser.getEmail().equals(user.getEmail()) && userMapper.existsByEmail(user.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }
        if (!existingUser.getPhone().equals(user.getPhone()) && userMapper.existsByPhone(user.getPhone())) {
            throw new BusinessException("手机号已被使用");
        }

        user.setUpdateTime(LocalDateTime.now());
        if (userMapper.update(user) <= 0) {
            throw new BusinessException("更新失败");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        redisUtil.setUserInfo(user.getId().toString(), userInfo);

        log.info("用户信息更新成功: userId={}", user.getId());
    }

    /**
     * 修改密码
     */
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        if (userMapper.update(user) <= 0) {
            throw new BusinessException("密码修改失败");
        }

        redisUtil.removeUserToken(userId.toString());
        log.info("密码修改成功: userId={}", userId);
    }

    /**
     * 获取用户列表
     */
    public List<User> getUserList(int page, int size) {
        return userMapper.findByPage((page - 1) * size, size);
    }

    /**
     * 获取用户总数
     */
    public int getUserCount() {
        return userMapper.count();
    }

    /**
     * 禁用用户
     */
    @Transactional
    public void disableUser(Long userId) {
        if (userMapper.findById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateStatus(userId, 0);
        redisUtil.removeUserToken(userId.toString());
        log.info("用户禁用成功: userId={}", userId);
    }

    /**
     * 启用用户
     */
    @Transactional
    public void enableUser(Long userId) {
        if (userMapper.findById(userId) == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateStatus(userId, 1);
        log.info("用户启用成功: userId={}", userId);
    }
}