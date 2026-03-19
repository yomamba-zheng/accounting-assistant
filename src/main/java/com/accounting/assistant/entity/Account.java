package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体类
 * 对应数据库表：t_account
 * 用于管理用户的多个账户（如现金、银行卡、支付宝等）
 */
@Data
@TableName("t_account")
public class Account {

    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 账户名称
     */
    private String name;

    /**
     * 账户类型：cash-现金, bank-银行卡, alipay-支付宝, wechat-微信, credit-信用卡, other-其他
     */
    private String type;

    /**
     * 当前余额
     */
    private BigDecimal balance;

    /**
     * 初始余额
     */
    private BigDecimal initialBalance;

    /**
     * 账户图标/emoji
     */
    private String icon;

    /**
     * 账户颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否默认账户
     */
    private Boolean isDefault;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;
}
