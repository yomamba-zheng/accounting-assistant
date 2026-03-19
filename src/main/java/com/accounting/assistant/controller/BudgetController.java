package com.accounting.assistant.controller;

import com.accounting.assistant.common.Result;
import com.accounting.assistant.entity.Budget;
import com.accounting.assistant.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "预算管理", description = "预算相关接口")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * 设置或更新预算
     */
    @Operation(summary = "设置预算", description = "设置或更新预算金额")
    @PostMapping
    public Result<Budget> setBudget(@Valid @RequestBody BudgetRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            Budget budget = budgetService.setBudget(
                    userId,
                    request.getAmount(),
                    request.getCategoryId(),
                    request.getPeriodType()
            );
            return Result.success("设置成功", budget);
        } catch (Exception e) {
            log.error("设置预算失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前预算列表
     */
    @Operation(summary = "获取预算列表", description = "获取当前周期的预算列表")
    @GetMapping
    public Result<List<Budget>> getBudgets(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : 1L;
        List<Budget> budgets = budgetService.getCurrentBudgets(currentUserId);
        return Result.success(budgets);
    }

    /**
     * 删除预算
     */
    @Operation(summary = "删除预算")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBudget(
            @Parameter(description = "预算ID") @PathVariable Long id,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        try {
            Long currentUserId = userId != null ? userId : 1L;
            budgetService.deleteBudget(id, currentUserId);
            return Result.successMessage("删除成功");
        } catch (Exception e) {
            log.error("删除预算失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查超支情况
     */
    @Operation(summary = "检查超支", description = "检查当前预算是否超支")
    @GetMapping("/check-overbudget")
    public Result<List<Budget>> checkOverBudget(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : 1L;
        List<Budget> overBudgetList = budgetService.checkOverBudget(currentUserId);
        return Result.success(overBudgetList);
    }

    /**
     * 预算请求对象
     */
    @lombok.Data
    public static class BudgetRequest {
        @Parameter(description = "用户ID")
        private Long userId;

        @Parameter(description = "预算金额")
        @NotNull(message = "金额不能为空")
        private BigDecimal amount;

        @Parameter(description = "分类ID（可选，为空表示总预算）")
        private Integer categoryId;

        @Parameter(description = "预算周期：monthly-月度, weekly-周度")
        private String periodType = "monthly";
    }
}
