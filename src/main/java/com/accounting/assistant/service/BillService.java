package com.accounting.assistant.service;

import com.accounting.assistant.entity.Bill;
import com.accounting.assistant.entity.Category;
import com.accounting.assistant.mapper.BillMapper;
import com.accounting.assistant.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final AmountParserService amountParserService;

    /**
     * 快速记账：自动解析金额和分类
     * 用户只需输入文本（如"午餐 15 元"），系统自动提取金额并智能分类
     * 
     * @param userId 用户 ID
     * @param text 记账文本，包含金额和分类信息
     * @param billTime 消费时间，可选，默认为当前时间
     * @param type 账单类型：1-支出，2-收入，可选
     * @return 创建成功的账单对象
     * @throws IllegalArgumentException 当金额解析失败或分类不可用时抛出
     */
    @Transactional
    public Bill quickAdd(Long userId, String text, LocalDateTime billTime, Integer type) {
        // 步骤 1：从文本中解析金额
        BigDecimal amount = amountParserService.extractAmount(text);
        // 验证金额的合法性（必须大于 0 且小于 100 万）
        if (!amountParserService.isValidAmount(amount)) {
            throw new IllegalArgumentException("无法解析有效金额，请确保金额格式正确，如：10 元、10.5 块");
        }

        // 步骤 2：智能分类（根据文本和时间）
        LocalDateTime actualTime = billTime != null ? billTime : LocalDateTime.now();
        Category category = categoryService.classify(text, actualTime.toLocalTime());
        // 一级容错：如果分类失败，使用默认分类
        if (category == null) {
            category = categoryService.getDefaultCategory();
        }
        // 二级容错：如果默认分类也没有，抛出异常
        if (category == null) {
            throw new IllegalArgumentException("系统没有可用的分类，请联系管理员");
        }

        // 步骤 3：创建账单记录
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(amount);
        bill.setCategoryId(category.getId());
        bill.setRemark(text);
        bill.setBillTime(actualTime);
        bill.setStatus(1); // 1-正常状态
        // 设置类型：1-支出，2-收入，默认使用分类的类型
        bill.setType(type != null ? type : (category.getType() != null ? category.getType() : 1));

        // 保存到数据库
        billMapper.insert(bill);

        // 关联分类信息（用于返回给前端）
        if (category.getParentId() != null) {
            Category parent = categoryMapper.selectById(category.getParentId());
            category.setParent(parent);
        }
        bill.setCategory(category);

        log.info("快速记账成功：用户{} 金额{} 分类{} 时间{} 类型{}", userId, amount, category.getName(), actualTime, bill.getType());
        return bill;
    }

    /**
     * 添加账单（手动指定分类）
     * 适用于用户明确知道分类的场景
     *
     * @param userId 用户 ID
     * @param amount 金额（必须大于 0）
     * @param categoryId 分类 ID
     * @param remark 备注信息
     * @param billTime 消费时间，可选
     * @return 创建成功的账单对象
     */
    @Transactional
    public Bill add(Long userId, BigDecimal amount, Integer categoryId, String remark, LocalDateTime billTime, Integer type) {
        // 参数校验：金额不能为空且必须大于 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }

        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }

        // 创建账单对象
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(amount);
        bill.setCategoryId(categoryId);
        bill.setRemark(remark);
        // 如果未提供时间，使用当前时间
        bill.setBillTime(billTime != null ? billTime : LocalDateTime.now());
        bill.setStatus(1);
        // 设置类型：1-支出，2-收入，默认使用分类的类型
        bill.setType(type != null ? type : (category.getType() != null ? category.getType() : 1));

        // 保存账单
        billMapper.insert(bill);
        if (category.getParentId() != null) {
            Category parent = categoryMapper.selectById(category.getParentId());
            category.setParent(parent);
        }
        bill.setCategory(category);

        log.info("添加账单成功：用户{} 金额{} 分类{} 类型{}", userId, amount, category.getName(), bill.getType());
        return bill;
    }

    /**
     * 更新账单
     * 支持部分字段更新（只更新非 null 字段）
     * 
     * @param billId 账单 ID
     * @param userId 用户 ID（用于权限校验）
     * @param amount 新金额（可选）
     * @param categoryId 新分类 ID（可选）
     * @param remark 新备注（可选）
     * @param billTime 新消费时间（可选）
     * @return 更新后的账单对象
     */
    @Transactional
    public Bill update(Long billId, Long userId, BigDecimal amount, Integer categoryId, String remark, LocalDateTime billTime) {
        // 查询原账单
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new IllegalArgumentException("账单不存在");
        }
        // 权限校验：只能修改自己的账单
        if (!bill.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此账单");
        }

        // 选择性更新：只更新非 null 字段
        if (amount != null) {
            bill.setAmount(amount);
        }
        if (categoryId != null) {
            Category category = categoryMapper.selectById(categoryId);
            if (category == null) {
                throw new IllegalArgumentException("分类不存在");
            }
            if (category.getParentId() != null) {
                Category parent = categoryMapper.selectById(category.getParentId());
                category.setParent(parent);
            }
            bill.setCategoryId(categoryId);
            bill.setCategory(category);
        }
        if (remark != null) {
            bill.setRemark(remark);
        }
        if (billTime != null) {
            bill.setBillTime(billTime);
        }

        // 执行更新
        billMapper.updateById(bill);
        log.info("更新账单成功：{}", billId);
        return bill;
    }

    /**
     * 删除账单（软删除）
     * 只将状态标记为 0（已删除），不从数据库中物理删除
     * 
     * @param billId 账单 ID
     * @param userId 用户 ID（用于权限校验）
     */
    @Transactional
    public void delete(Long billId, Long userId) {
        // 查询账单
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new IllegalArgumentException("账单不存在");
        }
        // 权限校验
        if (!bill.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此账单");
        }

        // 软删除：将状态设置为 0
        bill.setStatus(0);
        billMapper.updateById(bill);
        log.info("删除账单成功：{}", billId);
    }

    /**
     * 查询用户账单列表（分页）
     * 
     * @param userId 用户 ID
     * @param categoryId 分类 ID 过滤条件（可选）
     * @param startTime 开始时间过滤（可选）
     * @param endTime 结束时间过滤（可选）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 分页查询结果
     */
    public IPage<Bill> list(Long userId, Integer categoryId, LocalDateTime startTime,
                            LocalDateTime endTime, Integer page, Integer size) {
        // 构建查询条件
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)      // 必须：当前用户的账单
               .eq("status", 1)             // 必须：只查正常状态的账单
               .orderByDesc("bill_time");   // 按消费时间倒序排列

        // 动态添加过滤条件
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (startTime != null) {
            wrapper.ge("bill_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("bill_time", endTime);
        }

        // 执行分页查询
        Page<Bill> pageParam = new Page<>(page, size);
        IPage<Bill> billPage = billMapper.selectPage(pageParam, wrapper);

        // 填充分类信息（账单对象中的 category 字段是瞬态的，需要手动填充）
        for (Bill bill : billPage.getRecords()) {
            Category category = categoryMapper.selectById(bill.getCategoryId());
            if (category != null && category.getParentId() != null) {
                // 如果是小类，查询父分类
                Category parent = categoryMapper.selectById(category.getParentId());
                category.setParent(parent);
            }
            bill.setCategory(category);
        }

        return billPage;
    }

    /**
     * 获取账单详情
     * 
     * @param billId 账单 ID
     * @param userId 用户 ID（用于权限校验）
     * @return 账单对象（包含分类信息），如果不存在或无权限则返回 null
     */
    public Bill getById(Long billId, Long userId) {
        Bill bill = billMapper.selectById(billId);
        // 综合校验：账单不存在、不属于当前用户、或已被删除
        if (bill == null || !bill.getUserId().equals(userId) || bill.getStatus() == 0) {
            return null;
        }
        // 填充分类信息
        bill.setCategory(categoryMapper.selectById(bill.getCategoryId()));
        return bill;
    }

    /**
     * 统计用户消费总额
     * 
     * @param userId 用户 ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 消费总额
     */
    public BigDecimal getTotalExpense(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("status", 1);

        // 使用 bill.type = 1 来过滤支出（而不是依赖分类）
        wrapper.eq("type", 1);

        // 时间范围过滤
        if (startTime != null) {
            wrapper.ge("bill_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("bill_time", endTime);
        }

        // 查询所有符合条件的账单并求和
        List<Bill> bills = billMapper.selectList(wrapper);
        return bills.stream()
            .map(Bill::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 按分类统计消费
     * 返回按分类分组的消费金额统计，按金额降序排列
     * 
     * @param userId 用户 ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分类统计列表
     */
    public List<CategoryStat> getCategoryStats(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("status", 1)
               .eq("type", 1)  // 只统计支出
               .isNotNull("category_id");

        // 时间范围过滤
        if (startTime != null) {
            wrapper.ge("bill_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("bill_time", endTime);
        }

        // 查询所有账单（这里简化处理，实际可以用 SQL 聚合提高性能）
        List<Bill> bills = billMapper.selectList(wrapper);

        // 使用 Stream 进行分组聚合
        return bills.stream()
            // 按分类 ID 分组，对金额求和
            .collect(java.util.stream.Collectors.groupingBy(
                Bill::getCategoryId,
                java.util.stream.Collectors.reducing(
                    BigDecimal.ZERO,
                    Bill::getAmount,
                    BigDecimal::add
                )
            ))
            // 转换为统计对象
            .entrySet().stream()
            .map(entry -> {
                Category category = categoryMapper.selectById(entry.getKey());
                CategoryStat stat = new CategoryStat();
                stat.setCategoryId(entry.getKey());
                stat.setCategoryName(category != null ? category.getName() : "未知");
                stat.setCategoryColor(category != null ? category.getColor() : "#999999");
                stat.setTotalAmount(entry.getValue());
                return stat;
            })
            // 按消费金额降序排列
            .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
            .toList();
    }

    /**
     * 分类统计结果
     */
    @lombok.Data
    public static class CategoryStat {
        private Integer categoryId;
        private String categoryName;
        private String categoryColor;
        private BigDecimal totalAmount;
    }

    /**
     * 获取月度消费趋势（用于柱状图和折线图）
     * 返回最近N个月的月度消费数据
     *
     * @param userId 用户ID
     * @param months 查询月份数（默认6个月）
     * @return 月度趋势列表
     */
    public List<MonthlyTrend> getMonthlyTrends(Long userId, Integer months) {
        if (months == null || months <= 0) {
            months = 6;
        }

        List<MonthlyTrend> trends = new ArrayList<>();

        // 获取当前月份
        YearMonth currentMonth = YearMonth.now();

        // 查询最近N个月的数据
        for (int i = months - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDateTime startTime = targetMonth.atDay(1).atStartOfDay();
            LocalDateTime endTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);

            QueryWrapper<Bill> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                    .eq("status", 1)
                    .ge("bill_time", startTime)
                    .le("bill_time", endTime);

            List<Bill> bills = billMapper.selectList(wrapper);
            BigDecimal total = bills.stream()
                    .map(Bill::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            MonthlyTrend trend = new MonthlyTrend();
            trend.setYearMonth(targetMonth.toString());
            trend.setMonthLabel(targetMonth.getMonthValue() + "月");
            trend.setTotalAmount(total);
            trends.add(trend);
        }

        return trends;
    }

    /**
     * 获取分类占比（用于饼图）
     * 返回每个分类的消费占比
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分类占比列表
     */
    public List<CategoryPercentage> getCategoryPercentages(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 先获取分类统计
        List<CategoryStat> stats = getCategoryStats(userId, startTime, endTime);

        // 计算总额
        BigDecimal totalAmount = stats.stream()
                .map(CategoryStat::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 转换为百分比
        List<CategoryPercentage> percentages = new ArrayList<>();
        for (CategoryStat stat : stats) {
            CategoryPercentage pp = new CategoryPercentage();
            pp.setCategoryId(stat.getCategoryId());
            pp.setCategoryName(stat.getCategoryName());
            pp.setCategoryColor(stat.getCategoryColor());
            pp.setTotalAmount(stat.getTotalAmount());

            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                double percent = stat.getTotalAmount()
                        .divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                pp.setPercentage(percent);
            } else {
                pp.setPercentage(0.0);
            }

            percentages.add(pp);
        }

        return percentages;
    }

    /**
     * 月度趋势
     */
    @lombok.Data
    public static class MonthlyTrend {
        private String yearMonth;
        private String monthLabel;
        private BigDecimal totalAmount;
    }

    /**
     * 分类占比
     */
    @lombok.Data
    public static class CategoryPercentage {
        private Integer categoryId;
        private String categoryName;
        private String categoryColor;
        private BigDecimal totalAmount;
        private Double percentage;
    }
}
