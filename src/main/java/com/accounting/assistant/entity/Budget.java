package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算实体类
 * 对应数据库表：t_budget
 * 用于存储用户设置的预算信息
 */
@Data
@TableName("t_budget")
public class Budget {

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
     * 预算金额
     */
    private BigDecimal amount;

    /**
     * 分类 ID（可选，为空表示总预算）
     */
    private Integer categoryId;

    /**
     * 预算周期类型：monthly-月度, weekly-周度
     */
    private String periodType;

    /**
     * 预算开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预算结束时间
     */
    private LocalDateTime endTime;

    /**
     * 已使用金额（不含此字段，由查询计算）
     */
    @TableField(exist = false)
    private BigDecimal usedAmount;

    /**
     * 剩余金额（不含此字段，由查询计算）
     */
    @TableField(exist = false)
    private BigDecimal remainingAmount;

    /**
     * 使用百分比（不含此字段，由查询计算）
     */
    @TableField(exist = false)
    private Double usedPercent;

    /**
     * 是否超支
     */
    @TableField(exist = false)
    private Boolean isOverBudget;

    /**
     * 关联的分类对象
     */
    @TableField(exist = false)
    private Category category;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
}
