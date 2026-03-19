package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：t_user
 * 用于存储系统用户的基本信息和账户状态
 */
@Data
@TableName("t_user")
public class User {
    
    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名
     * 用户登录的唯一标识，不能重复
     */
    private String username;
    
    /**
     * 密码
     * 经过 BCrypt 加密后的密码，不存储明文
     * 示例：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH
     */
    private String password;
    
    /**
     * 电子邮箱
     * 用户的邮箱地址，可用于找回密码、接收通知等
     */
    private String email;
    
    /**
     * 手机号码
     * 用户的联系电话，可选字段
     */
    private String phone;
    
    /**
     * 创建时间
     * 用户注册的时间，由数据库自动维护
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * 用户信息最后一次修改的时间，由数据库自动维护
     */
    private LocalDateTime updatedAt;
    
    /**
     * 最后登录时间
     * 用户最后一次成功登录的时间，每次登录成功后更新
     */
    private LocalDateTime lastLogin;
    
    /**
     * 账户状态
     * 1-正常（可登录和使用），0-禁用（无法登录）
     */
    private Integer status;
}
