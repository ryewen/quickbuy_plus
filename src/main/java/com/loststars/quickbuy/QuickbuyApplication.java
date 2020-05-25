package com.loststars.quickbuy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.loststars.quickbuy.dao")
@EnableTransactionManagement
public class QuickbuyApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickbuyApplication.class, args);
	}

}
