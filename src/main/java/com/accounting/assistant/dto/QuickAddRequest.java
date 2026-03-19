package com.accounting.assistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "快速记账请求")
public class QuickAddRequest {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotBlank(message = "记账文本不能为空")
    @Schema(description = "记账文本，如：午餐15元", example = "午餐15元")
    private String text;

    @Schema(description = "消费时间，默认当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime billTime;

    @Schema(description = "账单类型：1-支出，2-收入", example = "1")
    private Integer type;
}
