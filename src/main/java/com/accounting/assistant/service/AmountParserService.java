package com.accounting.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AmountParserService {

    // 匹配各种金额格式
    private static final Pattern[] AMOUNT_PATTERNS = {
        // 匹配 "10元", "10.5元", "10块", "10.00块"
        Pattern.compile("(\\d+\\.?\\d*)\\s*[元块]"),
        // 匹配 "￥10", "￥10.5", "$10"
        Pattern.compile("[￥$¥]\\s*(\\d+\\.?\\d*)"),
        // 匹配 "10块钱", "10元整"
        Pattern.compile("(\\d+\\.?\\d*)\\s*块钱?"),
        // 匹配纯数字（通常是最后一个数字）
        Pattern.compile("(\\d+\\.?\\d*)\\s*$"),
        // 匹配中文数字: 十, 二十, 一百, 一百二十
        Pattern.compile("([一二三四五六七八九十百千]+)\\s*元?"),
    };

    // 中文数字到阿拉伯数字的映射
    private static final Map<Character, Integer> CN_NUM_MAP = new HashMap<>();

    static {
        CN_NUM_MAP.put('一', 1);
        CN_NUM_MAP.put('二', 2);
        CN_NUM_MAP.put('三', 3);
        CN_NUM_MAP.put('四', 4);
        CN_NUM_MAP.put('五', 5);
        CN_NUM_MAP.put('六', 6);
        CN_NUM_MAP.put('七', 7);
        CN_NUM_MAP.put('八', 8);
        CN_NUM_MAP.put('九', 9);
    }

    /**
     * 从文本中提取金额
     * 支持多种格式：10 元、10.5 块、￥10、$10、十元、一百等
     * 
     * @param text 包含金额的文本
     * @return 提取到的金额，如果未提取到则返回 0
     */
    public BigDecimal extractAmount(String text) {
        if (text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 第一阶段：尝试匹配带单位的金额（优先级从高到低）
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            BigDecimal amount = null;

            while (matcher.find()) {
                String numStr = matcher.group(1);
                if (numStr != null && !numStr.isEmpty()) {
                    // 判断是否为中文数字（如：十、二十、一百）
                    if (containsChineseNumber(numStr)) {
                        amount = parseChineseNumber(numStr);
                    } else {
                        // 普通阿拉伯数字
                        try {
                            amount = new BigDecimal(numStr);
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }

                    // 验证金额有效性（大于 0）
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        log.debug("提取金额：{} -> {}", text, amount);
                        return amount;
                    }
                }
            }
        }

        // 第二阶段：尝试提取纯数字（作为最后的手段）
        Pattern pureNumPattern = Pattern.compile("(\\d+\\.?\\d*)");
        Matcher matcher = pureNumPattern.matcher(text);
        BigDecimal lastAmount = null;

        // 提取最后一个合理的数字（防止匹配到日期、手机号等）
        while (matcher.find()) {
            try {
                BigDecimal amount = new BigDecimal(matcher.group(1));
                // 合理性检查：金额应该小于 10 万
                if (amount.compareTo(new BigDecimal("100000")) < 0) {
                    lastAmount = amount;
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        // 如果找到了合理的金额，返回最后一个
        if (lastAmount != null) {
            log.debug("提取纯数字金额：{} -> {}", text, lastAmount);
            return lastAmount;
        }

        // 完全无法提取时返回 0 并记录警告日志
        log.warn("未能在文本中提取到金额：{}", text);
        return BigDecimal.ZERO;
    }

    /**
     * 检查字符串是否包含中文数字
     */
    private boolean containsChineseNumber(String str) {
        return str.matches(".*[一二三四五六七八九十百千].*");
    }

    /**
     * 解析中文数字为阿拉伯数字
     * 支持：十、二十、一百、一百二十等
     * 
     * @param cnStr 中文数字字符串
     * @return 对应的阿拉伯数字
     */
    private BigDecimal parseChineseNumber(String cnStr) {
        try {
            // 去除可能的单位
            cnStr = cnStr.replace("元", "").trim();

            // 处理包含"十"的数字
            if (cnStr.contains("十")) {
                // 处理特殊情况："十" = 10
                if (cnStr.equals("十")) {
                    return new BigDecimal("10");
                }
                // 处理"十几"的情况：十六 = 16
                if (cnStr.startsWith("十")) {
                    return new BigDecimal("10" + parseChineseNumber(cnStr.substring(1)));
                }
                // 处理"几十"的情况：三十 = 30
                if (cnStr.endsWith("十")) {
                    return new BigDecimal(parseChineseNumber(cnStr.substring(0, cnStr.length() - 1)) + "0");
                }
            }

            // 处理包含"百"的数字
            if (cnStr.contains("百")) {
                String[] parts = cnStr.split("百");
                int result = 0;
                // 累加各部分的值
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        result += parseSimpleChineseNumber(part);
                    }
                }
                return new BigDecimal(result * 100);
            }

            // 简单情况：直接解析
            return new BigDecimal(parseSimpleChineseNumber(cnStr));
        } catch (Exception e) {
            log.warn("解析中文数字失败：{}", cnStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析简单中文数字（个位数）
     * 将一、二、三...映射为 1、2、3...
     * 
     * @param cnStr 中文数字字符
     * @return 对应的阿拉伯数字
     */
    private int parseSimpleChineseNumber(String cnStr) {
        int result = 0;
        int temp = 0;

        // 逐个字符解析
        for (char c : cnStr.toCharArray()) {
            if (CN_NUM_MAP.containsKey(c)) {
                if (temp == 0) {
                    temp = CN_NUM_MAP.get(c);
                } else {
                    // 处理组合情况（如：二十三 = 20 + 3）
                    result += temp * CN_NUM_MAP.get(c);
                    temp = 0;
                }
            }
        }
        result += temp;

        // 边界情况处理：空字符串返回 1
        return result == 0 && !cnStr.isEmpty() ? 1 : result;
    }

    /**
     * 验证金额是否合理
     * 用于快速记账时的金额校验
     * 
     * @param amount 待验证的金额
     * @return 是否合法（大于 0 且小于 100 万）
     */
    public boolean isValidAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        // 金额范围检查：0 < amount < 1,000,000
        return amount.compareTo(BigDecimal.ZERO) > 0
            && amount.compareTo(new BigDecimal("1000000")) < 0;
    }
}
