package com.ink.user.controller;

import com.ink.common.utils.Result;
import com.ink.user.dto.request.PasswordUpdateRequest;
import com.ink.user.dto.request.UserLoginRequest;
import com.ink.user.dto.request.UserRegisterRequest;
import com.ink.user.dto.request.UserUpdateRequest;
import com.ink.user.entity.User;
import com.ink.user.service.UserService;
import com.ink.user.util.ClientIpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户相关接口")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody UserRegisterRequest request) {
        User user = User.create(request.getUsername(), request.getPassword(),
                request.getEmail(), request.getPhone());
        userService.register(user);
        return Result.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = ClientIpUtil.getClientIp(httpRequest);
        Map<String, Object> data = userService.login(request.getUsername(), request.getPassword(), ip);
        return Result.success("登录成功", data);
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("X-User-Id") String userId) {
        userService.logout(userId);
        return Result.success("登出成功");
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("X-User-Id") String userId) {
        User user = userService.getUserById(Long.parseLong(userId));
        user.maskPassword();
        return Result.success(user);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<String> updateUserInfo(@RequestHeader("X-User-Id") String userId,
                                         @Valid @RequestBody UserUpdateRequest request) {
        User user = new User();
        user.setId(Long.parseLong(userId));
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        userService.updateUser(user);
        return Result.success("更新成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestHeader("X-User-Id") String userId,
                                         @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(Long.parseLong(userId), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    public Result<List<User>> getUserList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size) {
        List<User> users = userService.getUserList(page, size);
        users.forEach(User::maskPassword);
        return Result.success(users);
    }

    @Operation(summary = "获取用户总数")
    @GetMapping("/count")
    public Result<Integer> getUserCount() {
        return Result.success(userService.getUserCount());
    }

    @Operation(summary = "禁用用户")
    @PutMapping("/{userId}/disable")
    public Result<String> disableUser(@PathVariable @Parameter(description = "用户ID") Long userId) {
        userService.disableUser(userId);
        return Result.success("用户禁用成功");
    }

    @Operation(summary = "启用用户")
    @PutMapping("/{userId}/enable")
    public Result<String> enableUser(@PathVariable @Parameter(description = "用户ID") Long userId) {
        userService.enableUser(userId);
        return Result.success("用户启用成功");
    }

    @Operation(summary = "根据用户名获取用户信息")
    @GetMapping("/username/{username}")
    public Result<User> getUserByUsername(@PathVariable @Parameter(description = "用户名") String username) {
        User user = userService.getUserByUsername(username);
        user.maskPassword();
        return Result.success(user);
    }
}
