package com.eduardo.financialcontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinancialcontrolApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialcontrolApplication.class, args);
	}

}
