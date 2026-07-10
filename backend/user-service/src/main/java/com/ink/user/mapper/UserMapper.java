package com.ink.user.mapper;

import com.ink.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     */
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查询用户
     */
    User findByPhone(@Param("phone") String phone);
    
    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);
    
    /**
     * 查询所有用户
     */
    List<User> findAll();
    
    /**
     * 分页查询用户
     */
    List<User> findByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查询用户总数
     */
    int count();
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 删除用户
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新用户状态
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