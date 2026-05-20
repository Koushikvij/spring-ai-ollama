package com.koushik.course_catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import com.koushik.course_catalog.security.config.AppSecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppSecurityProperties.class)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class CourseCatalogApplication {
	public static void main(String[] args) {
		SpringApplication.run(CourseCatalogApplication.class, args);
	}

}
