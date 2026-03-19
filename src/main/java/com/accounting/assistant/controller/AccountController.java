package com.accounting.assistant.controller;

import com.accounting.assistant.common.Result;
import com.accounting.assistant.entity.Account;
import com.accounting.assistant.entity.AccountRecord;
import com.accounting.assistant.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "账户管理", description = "账户相关接口")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 获取账户列表
     */
    @Operation(summary = "获取账户列表", description = "获取用户的所有账户")
    @GetMapping
    public Result<List<Account>> getAccounts(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : 1L;
        List<Account> accounts = accountService.getUserAccounts(currentUserId);
        return Result.success(accounts);
    }

    /**
     * 获取默认账户
     */
    @Operation(summary = "获取默认账户")
    @GetMapping("/default")
    public Result<Account> getDefaultAccount(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : 1L;
        Account account = accountService.getDefaultAccount(currentUserId);
        return Result.success(account);
    }

    /**
     * 创建账户
     */
    @Operation(summary = "创建账户")
    @PostMapping
    public Result<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            Account account = accountService.createAccount(
                    userId,
                    request.getName(),
                    request.getType(),
                    request.getBalance(),
                    request.getIcon(),
                    request.getColor()
            );
            return Result.success("创建成功", account);
        } catch (Exception e) {
            log.error("创建账户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新账户
     */
    @Operation(summary = "更新账户")
    @PutMapping("/{id}")
    public Result<Account> updateAccount(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @RequestBody UpdateAccountRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            Account account = accountService.updateAccount(
                    id,
                    userId,
                    request.getName(),
                    request.getIcon(),
                    request.getColor(),
                    request.getIsDefault()
            );
            return Result.success("更新成功", account);
        } catch (Exception e) {
            log.error("更新账户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除账户
     */
    @Operation(summary = "删除账户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAccount(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        try {
            Long currentUserId = userId != null ? userId : 1L;
            accountService.deleteAccount(id, currentUserId);
            return Result.successMessage("删除成功");
        } catch (Exception e) {
            log.error("删除账户失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取账户变动记录
     */
    @Operation(summary = "获取账户变动记录")
    @GetMapping("/{id}/records")
    public Result<List<AccountRecord>> getAccountRecords(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "记录数量限制") @RequestParam(required = false) Integer limit) {
        try {
            Long currentUserId = userId != null ? userId : 1L;
            List<AccountRecord> records = accountService.getAccountRecords(id, currentUserId, limit);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取账户记录失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @lombok.Data
    public static class CreateAccountRequest {
        private Long userId;

        @NotBlank(message = "账户名称不能为空")
        private String name;

        private String type = "cash";
        private BigDecimal balance = BigDecimal.ZERO;
        private String icon;
        private String color;
    }

    @lombok.Data
    public static class UpdateAccountRequest {
        private Long userId;
        private String name;
        private String icon;
        private String color;
        private Boolean isDefault;
    }
}
