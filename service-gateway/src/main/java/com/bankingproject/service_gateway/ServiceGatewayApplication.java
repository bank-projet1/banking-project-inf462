package com.bankingproject.service_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServiceGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceGatewayApplication.class, args);
	}

	@Bean
	RouteLocator localLoanRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("service-loan", route -> route
						.path("/api/loans/**")
						.uri("lb://service-loan"))
				.build();
	}
}
