package com.example.construction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ConstructionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConstructionApplication.class, args);
	}

}
