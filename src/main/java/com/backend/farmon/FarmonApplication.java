package com.backend.farmon;

import com.backend.farmon.properties.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties(CorsProperties.class)
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class FarmonApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmonApplication.class, args);
	}

}
