package com.manageitout.candidate_tracker.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.manageitout.candidate_tracker.models.JobPosition;

public interface JobPositionRepository extends JpaRepository<JobPosition, Integer> {

	List<JobPosition> findByActive(boolean active);
}
