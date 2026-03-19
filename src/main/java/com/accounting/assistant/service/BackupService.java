package com.accounting.assistant.service;

import com.accounting.assistant.entity.*;
import com.accounting.assistant.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final UserMapper userMapper;
    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final BudgetMapper budgetMapper;
    private final AccountMapper accountMapper;
    private final AccountRecordMapper accountRecordMapper;

    /**
     * 导出用户数据
     */
    public Map<String, Object> exportData(Long userId) {
        Map<String, Object> data = new HashMap<>();

        // 导出用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            data.put("user", userData);
        }

        // 导出账单
        List<Bill> bills = billMapper.selectList(
                new QueryWrapper<Bill>().eq("user_id", userId)
        );
        List<Map<String, Object>> billList = new ArrayList<>();
        for (Bill bill : bills) {
            Map<String, Object> billMap = new HashMap<>();
            billMap.put("id", bill.getId());
            billMap.put("amount", bill.getAmount());
            billMap.put("categoryId", bill.getCategoryId());
            billMap.put("remark", bill.getRemark());
            billMap.put("billTime", bill.getBillTime());
            billMap.put("status", bill.getStatus());
            billList.add(billMap);
        }
        data.put("bills", billList);

        // 导出预算
        List<Budget> budgets = budgetMapper.selectList(
                new QueryWrapper<Budget>().eq("user_id", userId)
        );
        List<Map<String, Object>> budgetList = new ArrayList<>();
        for (Budget budget : budgets) {
            Map<String, Object> budgetMap = new HashMap<>();
            budgetMap.put("id", budget.getId());
            budgetMap.put("amount", budget.getAmount());
            budgetMap.put("categoryId", budget.getCategoryId());
            budgetMap.put("periodType", budget.getPeriodType());
            budgetMap.put("startTime", budget.getStartTime());
            budgetMap.put("endTime", budget.getEndTime());
            budgetMap.put("status", budget.getStatus());
            budgetList.add(budgetMap);
        }
        data.put("budgets", budgetList);

        // 导出账户
        List<Account> accounts = accountMapper.selectList(
                new QueryWrapper<Account>().eq("user_id", userId)
        );
        List<Map<String, Object>> accountList = new ArrayList<>();
        for (Account account : accounts) {
            Map<String, Object> accountMap = new HashMap<>();
            accountMap.put("id", account.getId());
            accountMap.put("name", account.getName());
            accountMap.put("type", account.getType());
            accountMap.put("balance", account.getBalance());
            accountMap.put("initialBalance", account.getInitialBalance());
            accountMap.put("icon", account.getIcon());
            accountMap.put("color", account.getColor());
            accountMap.put("sortOrder", account.getSortOrder());
            accountMap.put("isDefault", account.getIsDefault());
            accountMap.put("remark", account.getRemark());
            accountMap.put("status", account.getStatus());
            accountList.add(accountMap);
        }
        data.put("accounts", accountList);

        // 添加导出时间
        data.put("exportTime", LocalDateTime.now().toString());
        data.put("version", "1.0");

        return data;
    }

    /**
     * 导入用户数据
     */
    @Transactional
    public String importData(Long userId, Map<String, Object> data) {
        int importedCount = 0;

        // 导入账单
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bills = (List<Map<String, Object>>) data.get("bills");
        if (bills != null) {
            for (Map<String, Object> billData : bills) {
                Bill bill = new Bill();
                bill.setUserId(userId);
                bill.setAmount(new BigDecimal(billData.get("amount").toString()));
                bill.setCategoryId(Integer.parseInt(billData.get("categoryId").toString()));
                bill.setRemark((String) billData.get("remark"));
                bill.setBillTime(LocalDateTime.parse(billData.get("billTime").toString()));
                bill.setStatus(Integer.parseInt(billData.get("status").toString()));
                bill.setCreatedAt(LocalDateTime.now());
                bill.setUpdatedAt(LocalDateTime.now());
                billMapper.insert(bill);
                importedCount++;
            }
        }

        // 导入预算
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> budgets = (List<Map<String, Object>>) data.get("budgets");
        if (budgets != null) {
            for (Map<String, Object> budgetData : budgets) {
                Budget budget = new Budget();
                budget.setUserId(userId);
                budget.setAmount(new BigDecimal(budgetData.get("amount").toString()));
                if (budgetData.get("categoryId") != null) {
                    budget.setCategoryId(Integer.parseInt(budgetData.get("categoryId").toString()));
                }
                budget.setPeriodType((String) budgetData.get("periodType"));
                budget.setStartTime(LocalDateTime.parse(budgetData.get("startTime").toString()));
                budget.setEndTime(LocalDateTime.parse(budgetData.get("endTime").toString()));
                budget.setStatus(Integer.parseInt(budgetData.get("status").toString()));
                budget.setCreatedAt(LocalDateTime.now());
                budget.setUpdatedAt(LocalDateTime.now());
                budgetMapper.insert(budget);
                importedCount++;
            }
        }

        // 导入账户
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) data.get("accounts");
        if (accounts != null) {
            for (Map<String, Object> accountData : accounts) {
                Account account = new Account();
                account.setUserId(userId);
                account.setName((String) accountData.get("name"));
                account.setType((String) accountData.get("type"));
                account.setBalance(new BigDecimal(accountData.get("balance").toString()));
                account.setInitialBalance(new BigDecimal(accountData.get("initialBalance").toString()));
                account.setIcon((String) accountData.get("icon"));
                account.setColor((String) accountData.get("color"));
                account.setSortOrder(Integer.parseInt(accountData.get("sortOrder").toString()));
                account.setIsDefault((Boolean) accountData.get("isDefault"));
                account.setRemark((String) accountData.get("remark"));
                account.setStatus(Integer.parseInt(accountData.get("status").toString()));
                account.setCreatedAt(LocalDateTime.now());
                account.setUpdatedAt(LocalDateTime.now());
                accountMapper.insert(account);
                importedCount++;
            }
        }

        log.info("用户{} 导入数据成功，共导入{}条记录", userId, importedCount);
        return "导入成功，共导入 " + importedCount + " 条记录";
    }
}
