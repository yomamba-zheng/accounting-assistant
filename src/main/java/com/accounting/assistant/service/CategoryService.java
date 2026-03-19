package com.accounting.assistant.service;

import com.accounting.assistant.entity.Category;
import com.accounting.assistant.entity.CategoryRule;
import com.accounting.assistant.mapper.CategoryMapper;
import com.accounting.assistant.mapper.CategoryRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRuleMapper categoryRuleMapper;

    /**
     * 根据文本和消费时间智能分类
     * 三级分类策略：关键词匹配 > 时间上下文推断 > 用户习惯学习
     * 
     * @param text 账单描述文本，如"午餐 15 元"
     * @param billTime 消费时间，用于时间上下文判断
     * @return 匹配的分类对象，如果未匹配则返回默认分类
     */
    public Category classify(String text, LocalTime billTime) {
        // 空值处理：文本为空时返回默认分类
        if (text == null || text.isEmpty()) {
            return getDefaultCategory();
        }

        // 第一级：关键词精确匹配（优先级最高的规则）
        List<CategoryRule> exactRules = categoryRuleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CategoryRule>()
                .eq("is_active", true)
                .eq("priority", 1)
        );

        // 遍历所有高优先级规则
        for (CategoryRule rule : exactRules) {
            if (containsKeyword(text, rule.getKeyword())) {
                // 检查时间规则是否匹配（无时间限制的规则直接匹配）
                if (rule.getTimeStart() == null && rule.getTimeEnd() == null) {
                    Category category = categoryMapper.selectById(rule.getCategoryId());
                    if (category != null) {
                        log.debug("关键词匹配分类：{} -> {}", rule.getKeyword(), category.getName());
                        return category;
                    }
                }
            }
        }

        // 第二级：时间上下文推断（结合时间段和关键词）
        if (billTime != null) {
            List<CategoryRule> timeRules = categoryRuleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CategoryRule>()
                    .eq("is_active", true)
                    .isNotNull("time_start")
                    .isNotNull("time_end")
            );

            // 检查消费时间是否在规则的时间范围内
            for (CategoryRule rule : timeRules) {
                if (billTime.isAfter(rule.getTimeStart()) && billTime.isBefore(rule.getTimeEnd())) {
                    if (containsKeyword(text, rule.getKeyword())) {
                        Category category = categoryMapper.selectById(rule.getCategoryId());
                        if (category != null) {
                            log.debug("时间上下文匹配分类：{} @ {} -> {}", rule.getKeyword(), billTime, category.getName());
                            return category;
                        }
                    }
                }
            }
        }

        // 第三级：模糊匹配（不区分大小写的宽松匹配）
        for (CategoryRule rule : exactRules) {
            if (text.toLowerCase().contains(rule.getKeyword().toLowerCase())) {
                Category category = categoryMapper.selectById(rule.getCategoryId());
                if (category != null) {
                    log.debug("模糊匹配分类：{} -> {}", rule.getKeyword(), category.getName());
                    return category;
                }
            }
        }

        // 所有匹配都失败时返回默认分类
        return getDefaultCategory();
    }

    /**
     * 检查文本是否包含关键词（不区分大小写）
     * 
     * @param text 待检查的文本
     * @param keyword 关键词
     * @return 是否包含
     */
    private boolean containsKeyword(String text, String keyword) {
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 获取默认分类（其他）
     * 当无法匹配到合适分类时使用此默认分类
     * 
     * @return 默认分类对象
     */
    public Category getDefaultCategory() {
        // 优先查找名为"其他"的分类
        Category category = categoryMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category>()
                .eq("name", "其他")
                .eq("is_active", true)
        );
        // 如果没有"其他"分类，返回任意一个活跃分类作为兜底
        if (category == null) {
            category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category>()
                    .eq("is_active", true)
                    .last("LIMIT 1")
            );
        }
        return category;
    }

    /**
     * 根据ID获取分类
     *
     * @param id 分类ID
     * @return 分类对象
     */
    public Category getCategoryById(Integer id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 获取所有活跃分类
     */
    public List<Category> getAllActiveCategories() {
        return categoryMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category>()
                .eq("is_active", true)
                .orderByAsc("sort_order")
        );
    }

    /**
     * 根据类型获取分类
     */
    public List<Category> getCategoriesByType(Integer type) {
        return categoryMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category>()
                .eq("type", type)
                .eq("is_active", true)
                .orderByAsc("sort_order")
        );
    }
}
