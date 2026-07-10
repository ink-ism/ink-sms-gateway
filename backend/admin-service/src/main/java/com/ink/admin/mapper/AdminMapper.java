package com.ink.admin.mapper;

import com.ink.admin.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员Mapper接口
 */
@Mapper
public interface AdminMapper {
    
    /**
     * 根据用户名查询管理员
     */
    Admin findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询管理员
     */
    Admin findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查询管理员
     */
    Admin findByPhone(@Param("phone") String phone);
    
    /**
     * 根据ID查询管理员
     */
    Admin findById(@Param("id") Long id);
    
    /**
     * 查询所有管理员
     */
    List<Admin> findAll();
    
    /**
     * 分页查询管理员
     */
    List<Admin> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查询管理员总数
     */
    int count();
    
    /**
     * 插入管理员
     */
    int insert(Admin admin);
    
    /**
     * 更新管理员
     */
    int update(Admin admin);
    
    /**
     * 删除管理员
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新管理员状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime, @Param("lastLoginIp") String lastLoginIp);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(@Param("phone") String phone);
}
