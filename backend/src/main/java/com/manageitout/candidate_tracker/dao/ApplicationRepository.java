package com.manageitout.candidate_tracker.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.manageitout.candidate_tracker.models.Application;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

	Optional<Application> findFirstByEmailIgnoreCase(String email);

	@Transactional
	default Application saveOrUpdateByEmail(Application application) {
		return findFirstByEmailIgnoreCase(application.getEmail())
				.map(existing -> {
					existing.setName(application.getName());
					existing.setEmail(application.getEmail());
					existing.setPhoneNumber(application.getPhoneNumber());
					existing.setJobPosition(application.getJobPosition());
					existing.setRecommendation(application.getRecommendation());
					existing.setCvOriginalName(application.getCvOriginalName());
					existing.setCvStoredName(application.getCvStoredName());
					existing.setCvSizeBytes(application.getCvSizeBytes());
					existing.setMessage(application.getMessage());
					existing.setConsentGivenAt(application.getConsentGivenAt());
					return save(existing);
				})
				.orElseGet(() -> {
					application.setId(null);
					return save(application);
				});
	}
}
