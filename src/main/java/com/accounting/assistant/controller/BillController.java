package com.accounting.assistant.controller;

import com.accounting.assistant.common.Result;
import com.accounting.assistant.dto.AddBillRequest;
import com.accounting.assistant.dto.QuickAddPreviewResponse;
import com.accounting.assistant.dto.QuickAddRequest;
import com.accounting.assistant.dto.UpdateBillRequest;
import com.accounting.assistant.entity.Bill;
import com.accounting.assistant.entity.Category;
import com.accounting.assistant.service.AmountParserService;
import com.accounting.assistant.service.BillService;
import com.accounting.assistant.service.CategoryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Tag(name = "账单管理", description = "账单相关接口")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final CategoryService categoryService;
    private final AmountParserService amountParserService;
    
    // 用于存储最近提交的请求指纹（防止重复提交）
    private final ConcurrentHashMap<String, Long> recentRequests = new ConcurrentHashMap<>();
    private static final long REQUEST_DEDUPLICATION_WINDOW_MS = 3000; // 3 秒内重复请求会被拦截

    /**
     * 生成请求指纹（用于去重）
     * 
     * @param userId 用户 ID
     * @param text 记账文本
     * @param billTime 消费时间
     * @return 请求指纹字符串
     */
    private String generateRequestFingerprint(Long userId, String text, LocalDateTime billTime) {
        return String.format("%d_%s_%s", 
            userId, 
            text != null ? text.trim() : "", 
            billTime != null ? billTime.toString() : "now");
    }

    /**
     * 检查是否为重复请求
     * 
     * @param fingerprint 请求指纹
     * @return true-是重复请求，false-不是重复请求
     */
    private boolean isDuplicateRequest(String fingerprint) {
        Long lastRequestTime = recentRequests.get(fingerprint);
        long currentTime = System.currentTimeMillis();
        
        if (lastRequestTime != null && (currentTime - lastRequestTime) < REQUEST_DEDUPLICATION_WINDOW_MS) {
            log.warn("检测到重复请求，指纹：{}, 上次请求时间：{}", fingerprint, lastRequestTime);
            return true;
        }
        
        recentRequests.put(fingerprint, currentTime);
        
        // 清理过期的请求记录（超过 5 秒的）
        recentRequests.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > 5000);
        
        return false;
    }

    /**
     * 快速记账预览接口
     * 只返回解析结果，不保存账单，用于用户确认
     * 
     * @param text 记账文本
     * @param billTime 消费时间（可选）
     * @return 包含金额和分类的预览结果
     */
    @Operation(summary = "快速记账预览", description = "只返回解析结果，不保存账单，用于用户确认")
    @GetMapping("/quick-add/preview")
    public Result<QuickAddPreviewResponse> previewQuickAdd(
            @Parameter(description = "记账文本") @RequestParam String text,
            @Parameter(description = "消费时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime billTime) {
        try {
            // 解析金额
            var amount = amountParserService.extractAmount(text);

            // 智能分类
            LocalTime localTime = billTime != null ? billTime.toLocalTime() : LocalTime.now();
            Category category = categoryService.classify(text, localTime);

            // 填充父分类
            if (category != null && category.getParentId() != null) {
                Category parent = categoryService.getCategoryById(category.getParentId());
                category.setParent(parent);
            }

            QuickAddPreviewResponse response = new QuickAddPreviewResponse();
            response.setAmount(amount);
            response.setCategory(category);
            response.setRemark(text);
            response.setBillTime(billTime != null ? billTime : LocalDateTime.now());

            return Result.success(response);
        } catch (Exception e) {
            log.error("预览失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 快速记账接口
     * 用户输入文本（如"午餐 15 元"），系统自动提取金额和分类
     * 
     * @param request 请求参数，包含用户 ID、记账文本、消费时间
     * @return 创建成功的账单
     */
    @Operation(summary = "快速记账", description = "用户输入文本，自动提取金额和分类")
    @PostMapping("/quick-add")
    public Result<Bill> quickAdd(@Valid @RequestBody QuickAddRequest request) {
        try {
            // 默认使用用户 1 测试（生产环境应从登录信息中获取）
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            
            // 防止重复提交检查
            String fingerprint = generateRequestFingerprint(userId, request.getText(), request.getBillTime());
            if (isDuplicateRequest(fingerprint)) {
                log.warn("拒绝重复的记账请求：userId={}, text={}", userId, request.getText());
                return Result.error(409, "请勿重复提交相同的记账信息");
            }
            
            Bill bill = billService.quickAdd(userId, request.getText(), request.getBillTime(), request.getType());
            return Result.success("记账成功", bill);
        } catch (Exception e) {
            log.error("快速记账失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动添加账单接口
     * 用户明确指定金额、分类等信息
     * 
     * @param request 请求参数，包含用户 ID、金额、分类 ID、备注、消费时间
     * @return 创建成功的账单
     */
    @Operation(summary = "添加账单", description = "手动指定金额和分类")
    @PostMapping
    public Result<Bill> add(@Valid @RequestBody AddBillRequest request) {
        try {
            // 默认使用用户 1 测试
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            Bill bill = billService.add(userId, request.getAmount(), request.getCategoryId(),
                    request.getRemark(), request.getBillTime(), request.getType());
            return Result.success("添加成功", bill);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新账单")
    @PutMapping("/{id}")
    public Result<Bill> update(
            @Parameter(description = "账单ID") @PathVariable Long id,
            @RequestBody UpdateBillRequest request) {
        try {
            Long userId = 1L; // 默认测试用户
            Bill bill = billService.update(id, userId, request.getAmount(), request.getCategoryId(),
                    request.getRemark(), request.getBillTime());
            return Result.success("更新成功", bill);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除账单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "账单ID") @PathVariable Long id) {
        try {
            Long userId = 1L; // 默认测试用户
            billService.delete(id, userId);
            return Result.successMessage("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取账单列表")
    @GetMapping
    public Result<IPage<Bill>> list(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "分类ID") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = 1L; // 默认测试用户
        IPage<Bill> billPage = billService.list(userId, categoryId, startTime, endTime, page, size);
        return Result.success(billPage);
    }

    @Operation(summary = "获取账单详情")
    @GetMapping("/{id}")
    public Result<Bill> getById(@Parameter(description = "账单ID") @PathVariable Long id) {
        Long userId = 1L;
        Bill bill = billService.getById(id, userId);
        if (bill == null) {
            return Result.error("账单不存在");
        }
        return Result.success(bill);
    }

    @Operation(summary = "获取所有分类")
    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        List<Category> categories = categoryService.getAllActiveCategories();
        return Result.success(categories);
    }

    @Operation(summary = "按分类统计消费")
    @GetMapping("/statistics/category")
    public Result<List<BillService.CategoryStat>> getCategoryStats(
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = 1L;
        List<BillService.CategoryStat> stats = billService.getCategoryStats(userId, startTime, endTime);
        return Result.success(stats);
    }

    @Operation(summary = "获取消费总额")
    @GetMapping("/statistics/total")
    public Result<java.math.BigDecimal> getTotalExpense(
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = 1L;
        java.math.BigDecimal total = billService.getTotalExpense(userId, startTime, endTime);
        return Result.success(total);
    }

    /**
     * 获取统计数据（综合统计接口）
     * 返回消费总额和分类统计等完整数据
     * 
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 统计数据，包含总额和分类明细
     */
    @Operation(summary = "获取统计数据", description = "返回消费总额和分类统计等完整数据")
    @GetMapping("/statistics")
    public Result<StatResponse> getStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = 1L;
        
        StatResponse response = new StatResponse();
        // 获取消费总额
        response.setTotalExpense(billService.getTotalExpense(userId, startTime, endTime));
        // 获取分类统计
        response.setCategoryStats(billService.getCategoryStats(userId, startTime, endTime));
        
        return Result.success(response);
    }

    /**
     * 预览智能分类结果
     * 不保存账单，只返回分类建议，用于让用户确认分类是否正确
     * 
     * @param text 记账文本
     * @param billTime 消费时间（可选）
     * @return 建议的分类
     */
    @Operation(summary = "预览智能分类结果", description = "不保存账单，只返回分类建议")
    @GetMapping("/preview-classify")
    public Result<Category> previewClassify(
            @Parameter(description = "记账文本") @RequestParam String text,
            @Parameter(description = "消费时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm:ss") java.time.LocalTime billTime) {
        Category category = categoryService.classify(text, billTime);
        return Result.success(category);
    }

    /**
     * 预览金额解析结果
     * 不保存账单，只返回解析到的金额，用于让用户确认金额是否正确
     * 
     * @param text 记账文本
     * @return 解析到的金额
     */
    @Operation(summary = "解析金额", description = "不保存账单，只返回解析的金额")
    @GetMapping("/preview-amount")
    public Result<java.math.BigDecimal> previewAmount(
            @Parameter(description = "记账文本") @RequestParam String text) {
        java.math.BigDecimal amount = new com.accounting.assistant.service.AmountParserService()
                .extractAmount(text);
        return Result.success(amount);
    }

    /**
     * 统计数据响应对象
     */
    @lombok.Data
    public static class StatResponse {
        /**
         * 消费总额
         */
        private java.math.BigDecimal totalExpense;
        /**
         * 分类统计列表
         */
        private java.util.List<BillService.CategoryStat> categoryStats;
    }

    /**
     * 获取可视化数据
     * 返回饼图、柱状图、折线图所需的全部数据
     *
     * @param type 图表类型：pie-饼图, bar-柱状图, line-折线图, all-全部
     * @param months 查询月份数（用于趋势图）
     * @return 可视化数据
     */
    @Operation(summary = "获取可视化数据", description = "返回饼图、柱状图、折线图数据")
    @GetMapping("/statistics/visualization")
    public Result<VisualizationData> getVisualizationData(
            @Parameter(description = "图表类型：pie-饼图, bar-柱状图, line-折线图, all-全部") @RequestParam(defaultValue = "all") String type,
            @Parameter(description = "查询月份数") @RequestParam(defaultValue = "6") Integer months,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Long userId = 1L;

        VisualizationData data = new VisualizationData();

        // 如果没有指定时间范围，默认查询当月
        if (startTime == null && endTime == null) {
            LocalDateTime now = LocalDateTime.now();
            startTime = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            endTime = now;
        }

        // 饼图数据（分类占比）
        if ("pie".equals(type) || "all".equals(type)) {
            data.setCategoryPercentages(billService.getCategoryPercentages(userId, startTime, endTime));
        }

        // 柱状图和折线图数据（月度趋势）
        if ("bar".equals(type) || "line".equals(type) || "all".equals(type)) {
            data.setMonthlyTrends(billService.getMonthlyTrends(userId, months));
        }

        // 分类统计数据
        data.setCategoryStats(billService.getCategoryStats(userId, startTime, endTime));

        // 消费总额
        data.setTotalExpense(billService.getTotalExpense(userId, startTime, endTime));

        return Result.success(data);
    }

    /**
     * 可视化数据响应对象
     */
    @lombok.Data
    public static class VisualizationData {
        /**
         * 消费总额
         */
        private java.math.BigDecimal totalExpense;
        /**
         * 分类统计列表
         */
        private java.util.List<BillService.CategoryStat> categoryStats;
        /**
         * 分类占比（饼图数据）
         */
        private java.util.List<BillService.CategoryPercentage> categoryPercentages;
        /**
         * 月度趋势（柱状图/折线图数据）
         */
        private java.util.List<BillService.MonthlyTrend> monthlyTrends;
    }
}
