package com.manageitout.candidate_tracker.models;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "application")
public class Application {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "phone_number", nullable = false, length = 20)
	private String phoneNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_position_id", nullable = false)
	private JobPosition jobPosition;

	@Column(nullable = false, length = 100)
	private String recommendation;

	@Column(name = "cv_original_name", nullable = false, length = 255)
	private String cvOriginalName;

	@Column(name = "cv_stored_name", nullable = false, length = 40)
	private String cvStoredName;

	@Column(name = "cv_size_bytes", nullable = false)
	private Long cvSizeBytes;

	@Column(length = 1000)
	private String message;

	@Column(name = "consent_given_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime consentGivenAt;

	@Column(name = "received_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime receivedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private ApplicationStatus status = ApplicationStatus.NEW;

	public Application() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public JobPosition getJobPosition() {
		return jobPosition;
	}

	public void setJobPosition(JobPosition jobPosition) {
		this.jobPosition = jobPosition;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}

	public String getCvOriginalName() {
		return cvOriginalName;
	}

	public void setCvOriginalName(String cvOriginalName) {
		this.cvOriginalName = cvOriginalName;
	}

	public String getCvStoredName() {
		return cvStoredName;
	}

	public void setCvStoredName(String cvStoredName) {
		this.cvStoredName = cvStoredName;
	}

	public Long getCvSizeBytes() {
		return cvSizeBytes;
	}

	public void setCvSizeBytes(Long cvSizeBytes) {
		this.cvSizeBytes = cvSizeBytes;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public OffsetDateTime getConsentGivenAt() {
		return consentGivenAt;
	}

	public void setConsentGivenAt(OffsetDateTime consentGivenAt) {
		this.consentGivenAt = consentGivenAt;
	}

	public OffsetDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(OffsetDateTime receivedAt) {
		this.receivedAt = receivedAt;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public void setStatus(ApplicationStatus status) {
		this.status = status;
	}
}
