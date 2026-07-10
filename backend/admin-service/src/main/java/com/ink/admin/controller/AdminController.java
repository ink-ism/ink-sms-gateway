package com.ink.admin.controller;

import com.ink.common.utils.Result;
import com.ink.admin.dto.request.PasswordUpdateRequest;
import com.ink.admin.dto.request.AdminLoginRequest;
import com.ink.admin.dto.request.AdminCreateRequest;
import com.ink.admin.dto.request.AdminUpdateRequest;
import com.ink.admin.entity.Admin;
import com.ink.admin.service.AdminService;
import com.ink.admin.util.ClientIpUtil;
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
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员管理", description = "管理员相关接口")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "创建管理员（需管理员登录）")
    @PostMapping("/create")
    public Result<String> createAdmin(@RequestHeader("X-Admin-Id") String adminId,
                                      @Valid @RequestBody AdminCreateRequest request) {
        Admin admin = Admin.create(request.getUsername(), request.getPassword(),
                request.getEmail(), request.getPhone(), request.getRole());
        adminService.createAdmin(admin);
        return Result.success("创建成功");
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = ClientIpUtil.getClientIp(httpRequest);
        Map<String, Object> data = adminService.login(request.getUsername(), request.getPassword(), ip);
        return Result.success("登录成功", data);
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("X-Admin-Id") String adminId) {
        adminService.logout(adminId);
        return Result.success("登出成功");
    }

    @Operation(summary = "获取管理员信息")
    @GetMapping("/info")
    public Result<Admin> getAdminInfo(@RequestHeader("X-Admin-Id") String adminId) {
        Admin admin = adminService.getAdminById(Long.parseLong(adminId));
        admin.maskPassword();
        return Result.success(admin);
    }

    @Operation(summary = "更新管理员信息")
    @PutMapping("/info")
    public Result<String> updateAdminInfo(@RequestHeader("X-Admin-Id") String adminId,
                                         @Valid @RequestBody AdminUpdateRequest request) {
        Admin admin = new Admin();
        admin.setId(Long.parseLong(adminId));
        admin.setUsername(request.getUsername());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        admin.setNickname(request.getNickname());
        admin.setAvatar(request.getAvatar());
        admin.setRole(request.getRole());
        adminService.updateAdmin(admin);
        return Result.success("更新成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestHeader("X-Admin-Id") String adminId,
                                         @Valid @RequestBody PasswordUpdateRequest request) {
        adminService.updatePassword(Long.parseLong(adminId), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    @Operation(summary = "获取管理员列表")
    @GetMapping("/list")
    public Result<List<Admin>> getAdminList(
            @RequestParam(value = "page", defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(value = "size", defaultValue = "10") @Parameter(description = "每页大小") int size) {
        List<Admin> admins = adminService.getAdminList(page, size);
        admins.forEach(Admin::maskPassword);
        return Result.success(admins);
    }

    @Operation(summary = "获取管理员总数")
    @GetMapping("/count")
    public Result<Integer> getAdminCount() {
        return Result.success(adminService.getAdminCount());
    }

    @Operation(summary = "禁用管理员")
    @PutMapping("/{adminId}/disable")
    public Result<String> disableAdmin(@PathVariable @Parameter(description = "管理员ID") Long adminId) {
        adminService.disableAdmin(adminId);
        return Result.success("禁用成功");
    }

    @Operation(summary = "启用管理员")
    @PutMapping("/{adminId}/enable")
    public Result<String> enableAdmin(@PathVariable @Parameter(description = "管理员ID") Long adminId) {
        adminService.enableAdmin(adminId);
        return Result.success("启用成功");
    }

    @Operation(summary = "根据用户名获取管理员信息")
    @GetMapping("/username/{username}")
    public Result<Admin> getAdminByUsername(@PathVariable @Parameter(description = "用户名") String username) {
        Admin admin = adminService.getAdminByUsername(username);
        admin.maskPassword();
        return Result.success(admin);
    }

    @Operation(summary = "重置密码（临时接口）")
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestParam("username") String username, @RequestParam("newPassword") String newPassword) {
        adminService.resetPassword(username, newPassword);
        return Result.success("密码重置成功");
    }
}
