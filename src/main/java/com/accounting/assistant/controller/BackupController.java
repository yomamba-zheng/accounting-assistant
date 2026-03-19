package com.accounting.assistant.controller;

import com.accounting.assistant.common.Result;
import com.accounting.assistant.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "数据备份", description = "数据备份与恢复接口")
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    /**
     * 导出数据
     */
    @Operation(summary = "导出数据", description = "导出用户的所有数据为JSON格式")
    @GetMapping("/export")
    public Result<Map<String, Object>> exportData(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long currentUserId = userId != null ? userId : 1L;
        try {
            Map<String, Object> data = backupService.exportData(currentUserId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("导出数据失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导入数据
     */
    @Operation(summary = "导入数据", description = "从JSON数据导入")
    @PostMapping("/import")
    public Result<String> importData(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @RequestBody Map<String, Object> data) {
        Long currentUserId = userId != null ? userId : 1L;
        try {
            String result = backupService.importData(currentUserId, data);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入数据失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
