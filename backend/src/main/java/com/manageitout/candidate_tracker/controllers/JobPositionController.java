package com.manageitout.candidate_tracker.controllers;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.manageitout.candidate_tracker.dao.JobPositionRepository;
import com.manageitout.candidate_tracker.models.JobPosition;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Job positions", description = "Available job-position endpoints")
public class JobPositionController {

	private final JobPositionRepository jobPositionRepository;

	public JobPositionController(JobPositionRepository jobPositionRepository) {
		this.jobPositionRepository = jobPositionRepository;
	}

	@GetMapping
	@Operation(summary = "List job positions", description = "Returns all positions, optionally filtered by active status.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Job positions returned",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = JobPosition.class))))
	})
	public List<JobPosition> getPositions(
			@Parameter(description = "Filter by active status; omit to return both active and inactive positions.")
			@RequestParam(required = false) Boolean active) {
		return active == null
				? jobPositionRepository.findAll()
				: jobPositionRepository.findByActive(active);
	}
}
