package com.project.klare_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class KlareServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlareServerApplication.class, args);
	}

}
