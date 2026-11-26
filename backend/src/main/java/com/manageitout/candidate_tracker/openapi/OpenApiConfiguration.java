package com.manageitout.candidate_tracker.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI candidateTrackerOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Candidate Tracker API")
						.version("1.0.0")
						.description("API for job positions and candidate applications."));
	}
}
