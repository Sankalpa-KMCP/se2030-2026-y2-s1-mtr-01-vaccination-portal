package com.se2030.vaccinationportal;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaccinationPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaccinationPortalApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
