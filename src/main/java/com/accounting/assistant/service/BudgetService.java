package com.accounting.assistant.service;

import com.accounting.assistant.entity.Budget;
import com.accounting.assistant.entity.Category;
import com.accounting.assistant.mapper.BudgetMapper;
import com.accounting.assistant.mapper.BillMapper;
import com.accounting.assistant.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 设置或更新预算
     * 如果已存在同周期同分类的预算，则更新；否则新增
     */
    @Transactional
    public Budget setBudget(Long userId, BigDecimal amount, Integer categoryId, String periodType) {
        // 参数校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预算金额必须大于 0");
        }

        // 计算预算周期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime;

        if ("weekly".equals(periodType)) {
            // 周预算：从本周一 到 下周一
            startTime = now.with(LocalTime.MIN).minusDays(now.getDayOfWeek().getValue() - 1);
            endTime = startTime.plusDays(7);
        } else {
            // 默认月度预算
            YearMonth currentMonth = YearMonth.now();
            startTime = currentMonth.atDay(1).atStartOfDay();
            endTime = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        }

        // 查询是否已存在预算
        QueryWrapper<Budget> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("period_type", periodType)
                .eq("status", 1);

        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        } else {
            wrapper.isNull("category_id");
        }

        // 在时间范围内查询
        wrapper.le("start_time", now)
                .ge("end_time", now);

        Budget existingBudget = budgetMapper.selectOne(wrapper);

        if (existingBudget != null) {
            // 更新现有预算
            existingBudget.setAmount(amount);
            existingBudget.setStartTime(startTime);
            existingBudget.setEndTime(endTime);
            existingBudget.setUpdatedAt(now);
            budgetMapper.updateById(existingBudget);
            log.info("更新预算成功：用户{} 金额{} 分类{} 周期{}", userId, amount, categoryId, periodType);
            return getBudgetWithUsage(existingBudget);
        } else {
            // 新增预算
            Budget budget = new Budget();
            budget.setUserId(userId);
            budget.setAmount(amount);
            budget.setCategoryId(categoryId);
            budget.setPeriodType(periodType);
            budget.setStartTime(startTime);
            budget.setEndTime(endTime);
            budget.setStatus(1);
            budget.setCreatedAt(now);
            budget.setUpdatedAt(now);
            budgetMapper.insert(budget);
            log.info("新增预算成功：用户{} 金额{} 分类{} 周期{}", userId, amount, categoryId, periodType);
            return getBudgetWithUsage(budget);
        }
    }

    /**
     * 获取当前预算（包括使用情况）
     */
    public List<Budget> getCurrentBudgets(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<Budget> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .le("start_time", now)
                .ge("end_time", now)
                .orderByDesc("category_id"); // 总预算优先

        List<Budget> budgets = budgetMapper.selectList(wrapper);

        // 填充使用情况
        List<Budget> result = new ArrayList<>();
        for (Budget budget : budgets) {
            result.add(getBudgetWithUsage(budget));
        }

        return result;
    }

    /**
     * 获取预算使用情况
     */
    private Budget getBudgetWithUsage(Budget budget) {
        LocalDateTime startTime = budget.getStartTime();
        LocalDateTime endTime = budget.getEndTime();

        // 计算已使用金额
        QueryWrapper<com.accounting.assistant.entity.Bill> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", budget.getUserId())
                .eq("status", 1)
                .ge("bill_time", startTime)
                .le("bill_time", endTime);

        if (budget.getCategoryId() != null) {
            // 分类预算
            wrapper.eq("category_id", budget.getCategoryId());
        } else {
            // 总预算：只统计支出分类
            List<Category> expenseCategories = categoryMapper.selectList(
                    new QueryWrapper<Category>().eq("type", 1)
            );
            List<Integer> categoryIds = expenseCategories.stream()
                    .map(Category::getId)
                    .toList();
            if (!categoryIds.isEmpty()) {
                wrapper.in("category_id", categoryIds);
            }
        }

        List<com.accounting.assistant.entity.Bill> bills = billMapper.selectList(wrapper);
        BigDecimal usedAmount = bills.stream()
                .map(com.accounting.assistant.entity.Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算剩余金额和百分比
        BigDecimal remainingAmount = budget.getAmount().subtract(usedAmount);
        double usedPercent = 0;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = usedAmount.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        budget.setUsedAmount(usedAmount);
        budget.setRemainingAmount(remainingAmount);
        budget.setUsedPercent(usedPercent);
        budget.setIsOverBudget(remainingAmount.compareTo(BigDecimal.ZERO) < 0);

        // 填充分类信息
        if (budget.getCategoryId() != null) {
            budget.setCategory(categoryMapper.selectById(budget.getCategoryId()));
        }

        return budget;
    }

    /**
     * 删除预算
     */
    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetMapper.selectById(budgetId);
        if (budget == null) {
            throw new IllegalArgumentException("预算不存在");
        }
        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此预算");
        }

        // 软删除
        budget.setStatus(0);
        budgetMapper.updateById(budget);
        log.info("删除预算成功：{}", budgetId);
    }

    /**
     * 检查是否超支（用于超支预警）
     */
    public List<Budget> checkOverBudget(Long userId) {
        List<Budget> budgets = getCurrentBudgets(userId);
        List<Budget> overBudgetList = new ArrayList<>();

        for (Budget budget : budgets) {
            if (Boolean.TRUE.equals(budget.getIsOverBudget())) {
                overBudgetList.add(budget);
            }
        }

        return overBudgetList;
    }
}
