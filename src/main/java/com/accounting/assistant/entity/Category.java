package com.accounting.assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分类实体类
 * 对应数据库表：t_category
 * 用于定义账单的分类体系（如餐饮、交通、购物等）
 * 支持多级分类结构（通过 parentId 实现父子关系）
 */
@Data
@TableName("t_category")
public class Category {
    
    /**
     * 主键 ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 分类名称
     * 如"餐饮"、"交通"、"午餐"等，必须唯一
     */
    private String name;
    
    /**
     * 图标
     * 用于前端展示的 emoji 或图标标识，如🍔、🚗
     */
    private String icon;
    
    /**
     * 颜色代码
     * 用于前端展示的分类颜色，十六进制格式，如#FF6B6B
     */
    private String color;
    
    /**
     * 分类类型
     * 1-支出分类，2-收入分类
     * 用于区分是花钱还是赚钱的分类
     */
    private Integer type;
    
    /**
     * 父分类 ID
     * 用于构建多级分类树，如"午餐"的父分类是"餐饮"
     * 顶级分类的 parentId 为 NULL
     */
    private Integer parentId;
    
    /**
     * 是否活跃
     * true-启用，false-禁用
     * 禁用的分类不会在列表中显示，但历史账单仍保留引用
     */
    private Boolean isActive;
    
    /**
     * 排序顺序
     * 数值越小越靠前，用于控制分类在前端的显示顺序
     */
    private Integer sortOrder;
    
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
     * 父分类对象（非数据库字段，用于返回大类信息）
     */
    @TableField(exist = false)
    private Category parent;
}
