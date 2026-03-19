package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单实体类
 * 对应数据库表：t_bill
 * 用于存储用户的每一笔消费或收入记录
 */
@Data
@TableName("t_bill")
public class Bill {
    
    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户 ID
     * 关联 t_user 表的主键，标识这笔账单属于哪个用户
     */
    private Long userId;
    
    /**
     * 金额
     * 使用 BigDecimal 保证精度，避免浮点数计算误差
     */
    private BigDecimal amount;
    
    /**
     * 分类 ID
     * 关联 t_category 表的主键，标识这笔账单的分类（如餐饮、交通等）
     */
    private Integer categoryId;
    
    /**
     * 备注/描述
     * 存储原始记账文本或其他说明信息，如"午餐 15 元"
     */
    private String remark;
    
    /**
     * 消费时间
     * 实际发生消费的时间，可以与创建时间不同
     */
    private LocalDateTime billTime;
    
    /**
     * 创建时间
     * 记录插入数据库的时间，由数据库自动维护
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * 记录最后一次更新的时间，由数据库自动维护
     */
    private LocalDateTime updatedAt;
    
    /**
     * 状态
     * 1-正常，0-已删除（软删除标记）
     */
    private Integer status;

    /**
     * 账单类型
     * 1-支出，2-收入
     */
    private Integer type;

    /**
     * 关联的分类对象
     * 该字段不存在于数据库表中，用于查询时填充分类的详细信息
     */
    @TableField(exist = false)
    private Category category;
}
