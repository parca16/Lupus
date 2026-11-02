package com.parca16.lupus.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Reloj del sistema y planificador de tareas (limpieza de salas). */
@Configuration
@EnableScheduling
public class TimeConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
