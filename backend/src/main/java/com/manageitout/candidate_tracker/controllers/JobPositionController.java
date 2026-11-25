package com.manageitout.candidate_tracker.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.manageitout.candidate_tracker.dao.JobPositionRepository;
import com.manageitout.candidate_tracker.models.JobPosition;

@RestController
@RequestMapping("/api/positions")
public class JobPositionController {

	private final JobPositionRepository jobPositionRepository;

	public JobPositionController(JobPositionRepository jobPositionRepository) {
		this.jobPositionRepository = jobPositionRepository;
	}

	@GetMapping
	public List<JobPosition> getPositions(@RequestParam(required = false) Boolean active) {
		return active == null
				? jobPositionRepository.findAll()
				: jobPositionRepository.findByActive(active);
	}
}
