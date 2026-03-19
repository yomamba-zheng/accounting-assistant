package com.accounting.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.accounting.assistant.mapper")
public class AccountingAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountingAssistantApplication.class, args);
    }
}
