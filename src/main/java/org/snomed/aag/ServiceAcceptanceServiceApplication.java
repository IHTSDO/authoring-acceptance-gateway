package org.snomed.aag;

import org.snomed.aag.config.Config;
import org.springframework.boot.SpringApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
public class ServiceAcceptanceServiceApplication extends Config {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAcceptanceServiceApplication.class, args);
	}

}
