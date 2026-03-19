package com.accounting.assistant.service;

import com.accounting.assistant.entity.Account;
import com.accounting.assistant.entity.AccountRecord;
import com.accounting.assistant.mapper.AccountMapper;
import com.accounting.assistant.mapper.AccountRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final AccountRecordMapper accountRecordMapper;

    /**
     * 获取用户的所有账户
     */
    public List<Account> getUserAccounts(Long userId) {
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .orderByAsc("sort_order")
                .orderByDesc("is_default");
        return accountMapper.selectList(wrapper);
    }

    /**
     * 获取默认账户
     */
    public Account getDefaultAccount(Long userId) {
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .eq("is_default", true)
                .last("LIMIT 1");
        Account account = accountMapper.selectOne(wrapper);

        // 如果没有默认账户，返回第一个
        if (account == null) {
            wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId)
                    .eq("status", 1)
                    .orderByAsc("sort_order")
                    .last("LIMIT 1");
            account = accountMapper.selectOne(wrapper);
        }

        return account;
    }

    /**
     * 创建账户
     */
    @Transactional
    public Account createAccount(Long userId, String name, String type, BigDecimal balance, String icon, String color) {
        // 检查是否已有账户
        long count = accountMapper.selectCount(
                new QueryWrapper<Account>().eq("user_id", userId)
        );

        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType(type);
        account.setBalance(balance);
        account.setInitialBalance(balance);
        account.setIcon(icon != null ? icon : getDefaultIcon(type));
        account.setColor(color != null ? color : getDefaultColor(type));
        account.setSortOrder((int) count);
        // 第一个账户默认为默认账户
        account.setIsDefault(count == 0);
        account.setStatus(1);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        accountMapper.insert(account);
        log.info("创建账户成功：用户{} 账户{} 余额{}", userId, name, balance);

        return account;
    }

    /**
     * 更新账户
     */
    @Transactional
    public Account updateAccount(Long accountId, Long userId, String name, String icon, String color, Boolean isDefault) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此账户");
        }

        if (name != null) {
            account.setName(name);
        }
        if (icon != null) {
            account.setIcon(icon);
        }
        if (color != null) {
            account.setColor(color);
        }
        if (isDefault != null && isDefault) {
            // 取消其他默认账户
            UpdateWrapper<Account> wrapper = new UpdateWrapper<>();
            wrapper.eq("user_id", userId).eq("is_default", true);
            wrapper.set("is_default", false);
            accountMapper.update(null, wrapper);

            account.setIsDefault(true);
        }

        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);

        return account;
    }

    /**
     * 删除账户（软删除）
     */
    @Transactional
    public void deleteAccount(Long accountId, Long userId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此账户");
        }

        account.setStatus(0);
        accountMapper.updateById(account);

        // 如果删除的是默认账户，指定另一个为默认账户
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            Account newDefault = accountMapper.selectOne(
                    new QueryWrapper<Account>()
                            .eq("user_id", userId)
                            .eq("status", 1)
                            .ne("id", accountId)
                            .orderByAsc("sort_order")
                            .last("LIMIT 1")
            );
            if (newDefault != null) {
                newDefault.setIsDefault(true);
                accountMapper.updateById(newDefault);
            }
        }

        log.info("删除账户成功：{}", accountId);
    }

    /**
     * 账户余额变动
     */
    @Transactional
    public void changeBalance(Long accountId, Long userId, BigDecimal amount, String type, Long billId, String remark) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作此账户");
        }

        BigDecimal beforeBalance = account.getBalance();
        BigDecimal afterBalance = beforeBalance.add(amount);

        if (afterBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("余额不足");
        }

        // 更新账户余额
        account.setBalance(afterBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);

        // 记录变动
        AccountRecord record = new AccountRecord();
        record.setAccountId(accountId);
        record.setUserId(userId);
        record.setBillId(billId);
        record.setAmount(amount);
        record.setBeforeBalance(beforeBalance);
        record.setAfterBalance(afterBalance);
        record.setType(type);
        record.setRemark(remark);
        record.setCreatedAt(LocalDateTime.now());

        accountRecordMapper.insert(record);

        log.info("账户余额变动：账户{} 变动{} 余额{}->{}", accountId, amount, beforeBalance, afterBalance);
    }

    /**
     * 获取账户变动记录
     */
    public List<AccountRecord> getAccountRecords(Long accountId, Long userId, Integer limit) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || !account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("账户不存在");
        }

        QueryWrapper<AccountRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("account_id", accountId)
                .orderByDesc("created_at");

        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        return accountRecordMapper.selectList(wrapper);
    }

    private String getDefaultIcon(String type) {
        return switch (type) {
            case "cash" -> "💵";
            case "bank" -> "🏦";
            case "alipay" -> "💳";
            case "wechat" -> "💬";
            case "credit" -> "💳";
            default -> "💰";
        };
    }

    private String getDefaultColor(String type) {
        return switch (type) {
            case "cash" -> "#52c41a";
            case "bank" -> "#1890ff";
            case "alipay" -> "#1890ff";
            case "wechat" -> "#07c160";
            case "credit" -> "#fa8c16";
            default -> "#999999";
        };
    }
}
