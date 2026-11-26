package com.manageitout.candidate_tracker.openapi;

import java.time.OffsetDateTime;

import com.manageitout.candidate_tracker.models.ApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public final class OpenApiSchemas {

	private OpenApiSchemas() {
	}

	@Schema(name = "ApplicationSubmissionForm", description = "Multipart form parts used to submit an application.")
	public record ApplicationSubmissionForm(
			@Schema(description = "Application details encoded as JSON.", requiredMode = Schema.RequiredMode.REQUIRED)
			ApplicationDetails application,
			@Schema(
					description = "Candidate CV as a PDF file, up to 5 MB.",
					type = "string",
					format = "binary",
					requiredMode = Schema.RequiredMode.REQUIRED)
			byte[] cv) {
	}

	@Schema(name = "ApplicationDetails", description = "Candidate and application details.")
	public record ApplicationDetails(
			@Schema(
					description = "Candidate's full name.",
					example = "Alex Candidate",
					minLength = 2,
					maxLength = 100,
					requiredMode = Schema.RequiredMode.REQUIRED)
			String name,
			@Schema(
					description = "Candidate's email address.",
					example = "alex@example.com",
					format = "email",
					maxLength = 254,
					requiredMode = Schema.RequiredMode.REQUIRED)
			String email,
			@Schema(
					description = "Candidate's phone number.",
					example = "+381601234567",
					minLength = 6,
					maxLength = 20,
					requiredMode = Schema.RequiredMode.REQUIRED)
			String phone_number,
			@Schema(description = "ID of the selected job position; may be null.", example = "1", minimum = "1", nullable = true)
			Integer job_position_id,
			@Schema(
					description = "How the candidate heard about the position.",
					example = "LinkedIn",
					minLength = 2,
					maxLength = 100,
					requiredMode = Schema.RequiredMode.REQUIRED)
			String recommendation,
			@Schema(
					description = "Optional message, up to 1000 characters.",
					example = "I look forward to hearing from you.",
					maxLength = 1000)
			String message,
			@Schema(
					description = "Time when the candidate gave consent, in ISO 8601 date-time format.",
					type = "string",
					format = "date-time",
					example = "2026-03-14T10:15:30Z",
					requiredMode = Schema.RequiredMode.REQUIRED)
			OffsetDateTime consent_given_at) {
	}

	@Schema(name = "ApplicationSubmissionResponse", description = "Identifier and status returned after submission.")
	public record ApplicationSubmissionResponse(
			@Schema(description = "Saved application ID.", example = "42")
			Integer id,
			@Schema(description = "Current application status.", example = "NEW")
			ApplicationStatus status,
			@Schema(description = "When the application was received.", type = "string", format = "date-time")
			OffsetDateTime received_at) {
	}
}
