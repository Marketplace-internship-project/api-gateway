package io.hohichh.marketplace.appname;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableCaching
public class Application {
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
