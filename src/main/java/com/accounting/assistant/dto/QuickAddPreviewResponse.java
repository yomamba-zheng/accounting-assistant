package com.accounting.assistant.dto;

import com.accounting.assistant.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快速记账预览响应对象
 * 用于返回解析结果但不保存账单
 */
@Data
@Schema(description = "快速记账预览响应")
public class QuickAddPreviewResponse {

    @Schema(description = "解析到的金额")
    private BigDecimal amount;

    @Schema(description = "建议的分类")
    private Category category;

    @Schema(description = "原始文本")
    private String remark;

    @Schema(description = "消费时间")
    private LocalDateTime billTime;
}
