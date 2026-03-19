package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户变动记录实体类
 * 对应数据库表：t_account_record
 * 记录账户余额的每一笔变动
 */
@Data
@TableName("t_account_record")
public class AccountRecord {

    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账户 ID
     */
    private Long accountId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联的账单 ID（可选）
     */
    private Long billId;

    /**
     * 变动金额（正数增加，负数减少）
     */
    private BigDecimal amount;

    /**
     * 变动前余额
     */
    private BigDecimal beforeBalance;

    /**
     * 变动后余额
     */
    private BigDecimal afterBalance;

    /**
     * 变动类型：income-收入, expense-支出, transfer-转账, adjust-调整
     */
    private String type;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
