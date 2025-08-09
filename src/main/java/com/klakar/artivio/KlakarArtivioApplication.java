package com.klakar.artivio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class KlakarArtivioApplication {
	public static void main(String[] args) {
		SpringApplication.run(KlakarArtivioApplication.class, args);
	}
}