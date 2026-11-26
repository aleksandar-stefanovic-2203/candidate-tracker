package com.manageitout.candidate_tracker.controllers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.manageitout.candidate_tracker.dao.ApplicationRepository;
import com.manageitout.candidate_tracker.dao.JobPositionRepository;
import com.manageitout.candidate_tracker.models.Application;
import com.manageitout.candidate_tracker.models.ApplicationStatus;
import com.manageitout.candidate_tracker.models.JobPosition;
import com.manageitout.candidate_tracker.openapi.OpenApiSchemas;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Application submission and review endpoints")
public class ApplicationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);
	private static final long MAX_CV_SIZE_BYTES = 5L * 1024 * 1024;
	private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Path CV_DIRECTORY = Path.of("uploads").toAbsolutePath().normalize();

	private final ApplicationRepository applicationRepository;
	private final JobPositionRepository jobPositionRepository;

	public ApplicationController(
			ApplicationRepository applicationRepository,
			JobPositionRepository jobPositionRepository) {
		this.applicationRepository = applicationRepository;
		this.jobPositionRepository = jobPositionRepository;
	}

	@GetMapping
	@Operation(summary = "List applications", description = "Returns all submitted applications.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Applications returned",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = Application.class))))
	})
	public List<Application> getApplications() {
		return applicationRepository.findAll();
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "Submit or update an application",
			description = """
					Submits a candidate application or replaces the existing application with the same email address.
					The `application` part is JSON. Required fields are `name` (2-100 characters), `email` (valid
					address, up to 254 characters), `phone_number` (6-20 characters), `recommendation`
					(2-100 characters), and `consent_given_at` (ISO 8601 date-time). `job_position_id` and `message`
					are optional; the message may be up to 1000 characters. The required `cv` part must be a PDF no
					larger than 5 MB.
					""",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
							schema = @Schema(implementation = OpenApiSchemas.ApplicationSubmissionForm.class),
							encoding = @Encoding(name = "application", contentType = MediaType.APPLICATION_JSON_VALUE))))
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Existing application updated",
					content = @Content(schema = @Schema(implementation = OpenApiSchemas.ApplicationSubmissionResponse.class))),
			@ApiResponse(
					responseCode = "201",
					description = "New application created",
					content = @Content(schema = @Schema(implementation = OpenApiSchemas.ApplicationSubmissionResponse.class))),
			@ApiResponse(responseCode = "400", description = "Application fields or uploaded file are invalid"),
			@ApiResponse(responseCode = "404", description = "The specified job position does not exist or is inactive"),
			@ApiResponse(responseCode = "413", description = "The CV exceeds the 5 MB limit"),
			@ApiResponse(responseCode = "415", description = "The CV is not a PDF")
	})
	public ResponseEntity<Map<String, Object>> submitApplication(
			@RequestPart("application") Map<String, Object> form,
			@RequestPart("cv") MultipartFile cv) {
		String name = requiredText(form, "name", 2, 100);
		String email = requiredText(form, "email", 1, 254);
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
		}
		String phoneNumber = requiredText(form, "phone_number", 6, 20);
		String recommendation = requiredText(form, "recommendation", 2, 100);
		String message = optionalText(form, "message", 1000);
		OffsetDateTime consentGivenAt = parseConsentTime(form.get("consent_given_at"));
		Integer jobPositionId = parseOptionalJobPositionId(form.get("job_position_id"));
		JobPosition jobPosition = null;
		if (jobPositionId != null) {
			jobPosition = jobPositionRepository.findById(jobPositionId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job position not found"));
			if (!jobPosition.isActive()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job position is inactive");
			}
		}
		Application existing = applicationRepository.findFirstByEmailIgnoreCase(email).orElse(null);
		String previousCvStoredName = existing == null ? null : existing.getCvStoredName();

		String originalFilename = getOriginalFilename(cv);
		byte[] cvContents = readPdf(cv, originalFilename);
		String storedFilename = UUID.randomUUID() + ".pdf";
		Path storedFile = CV_DIRECTORY.resolve(storedFilename);
		writeCv(cvContents, storedFile);

		Application application = new Application();
		application.setName(name);
		application.setEmail(email);
		application.setPhoneNumber(phoneNumber);
		application.setJobPosition(jobPosition);
		application.setRecommendation(recommendation);
		application.setCvOriginalName(originalFilename);
		application.setCvStoredName(storedFilename);
		application.setCvSizeBytes(cv.getSize());
		application.setMessage(message);
		application.setConsentGivenAt(consentGivenAt);
		application.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
		application.setStatus(ApplicationStatus.NEW);

		Application saved;
		try {
			saved = applicationRepository.saveOrUpdateByEmail(application);
		} catch (RuntimeException exception) {
			try {
				Files.deleteIfExists(storedFile);
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}

		if (previousCvStoredName != null) {
			deletePreviousCv(previousCvStoredName);
		}
		HttpStatus responseStatus = existing == null ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(responseStatus)
				.body(Map.of(
						"id", saved.getId(),
						"status", saved.getStatus(),
						"received_at", saved.getReceivedAt()));
	}

	private String requiredText(Map<String, Object> form, String field, int minLength, int maxLength) {
		Object value = form.get(field);
		if (!(value instanceof String text)
				|| text.isBlank()
				|| text.length() < minLength
				|| text.length() > maxLength) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field);
		}
		return text;
	}

	private String optionalText(Map<String, Object> form, String field, int maxLength) {
		Object value = form.get(field);
		if (value == null) {
			return null;
		}
		if (!(value instanceof String text) || text.length() > maxLength) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field);
		}
		return text;
	}

	private OffsetDateTime parseConsentTime(Object value) {
		if (!(value instanceof String text)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent_given_at is required");
		}
		try {
			return OffsetDateTime.parse(text);
		} catch (java.time.format.DateTimeParseException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consent_given_at", exception);
		}
	}

	private Integer parseOptionalJobPositionId(Object value) {
		if (value == null || (value instanceof String text && text.isBlank())) {
			return null;
		}
		try {
			int id = Integer.parseInt(String.valueOf(value));
			if (id > 0) {
				return id;
			}
		} catch (NumberFormatException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job_position_id", exception);
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job_position_id");
	}

	private String getOriginalFilename(MultipartFile cv) {
		if (cv.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV file is required");
		}
		if (cv.getSize() > MAX_CV_SIZE_BYTES) {
			throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "CV must not exceed 5 MB");
		}
		String submittedName = cv.getOriginalFilename();
		if (submittedName == null || submittedName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV filename is required");
		}
		String normalizedName = submittedName.replace('\\', '/');
		String filename = normalizedName.substring(normalizedName.lastIndexOf('/') + 1).trim();
		if (filename.isEmpty() || filename.length() > 255) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CV filename");
		}
		if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CV must be a PDF");
		}
		return filename;
	}

	private byte[] readPdf(MultipartFile cv, String filename) {
		try {
			byte[] contents = cv.getBytes();
			if (contents.length < PDF_SIGNATURE.length
					|| !Arrays.equals(Arrays.copyOf(contents, PDF_SIGNATURE.length), PDF_SIGNATURE)) {
				throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CV must be a PDF");
			}
			return contents;
		} catch (IOException exception) {
			throw new UncheckedIOException("Could not read uploaded CV: " + filename, exception);
		}
	}

	private void writeCv(byte[] contents, Path destination) {
		try {
			Files.createDirectories(CV_DIRECTORY);
			Files.write(
					destination,
					contents,
					StandardOpenOption.CREATE_NEW,
					StandardOpenOption.WRITE);
		} catch (IOException exception) {
			try {
				Files.deleteIfExists(destination);
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw new UncheckedIOException("Could not store uploaded CV", exception);
		}
	}

	private void deletePreviousCv(String storedFilename) {
		Path previousFile = CV_DIRECTORY.resolve(storedFilename).normalize();
		if (!CV_DIRECTORY.equals(previousFile.getParent())) {
			return;
		}
		try {
			Files.deleteIfExists(previousFile);
		} catch (IOException exception) {
			LOGGER.warn("Could not remove replaced CV file {}", previousFile, exception);
		}
	}
}
