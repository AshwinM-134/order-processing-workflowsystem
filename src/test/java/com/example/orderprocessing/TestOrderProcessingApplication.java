package com.example.orderprocessing;

import org.springframework.boot.SpringApplication;

public class TestOrderProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderProcessingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
