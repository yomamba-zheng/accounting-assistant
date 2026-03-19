package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 分类规则实体类
 * 对应数据库表：t_category_rule
 * 用于定义关键词与分类之间的映射规则
 * 支持基于时间段和时间上下文的智能分类
 */
@Data
@TableName("t_category_rule")
public class CategoryRule {
    
    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 分类 ID
     * 关联 t_category 表的主键，匹配到该规则后会将账单归入此分类
     */
    private Integer categoryId;
    
    /**
     * 关键词
     * 用于匹配账单描述的关键词，如"午餐"、"公交"等
     * 不区分大小写进行匹配
     */
    private String keyword;
    
    /**
     * 时间范围开始
     * 用于时间上下文规则，如 11:00-13:00 之间提到"饭"归类为午餐
     * 如果为 NULL，表示该规则不受时间限制
     */
    private LocalTime timeStart;
    
    /**
     * 时间范围结束
     * 与 timeStart 配合使用，定义时间上下文的有效范围
     * 如果为 NULL，表示该规则不受时间限制
     */
    private LocalTime timeEnd;
    
    /**
     * 优先级
     * 0-低优先级（通常用于时间上下文规则）
     * 1-高优先级（通常用于关键词精确匹配）
     * 数字越大优先级越高
     */
    private Integer priority;
    
    /**
     * 是否活跃
     * true-启用，false-禁用
     * 禁用的规则不会被用于分类匹配
     */
    private Boolean isActive;
    
    /**
     * 用户 ID
     * 关联 t_user 表的主键，用于实现用户自定义规则
     * 如果为 NULL，表示这是系统默认规则
     */
    private Long userId;
    
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
}
