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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/applications")
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

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> submitApplication(
			@RequestPart("application") Map<String, Object> form,
			@RequestPart("cv") MultipartFile cv) {
		String name = requiredText(form, "name", 2, 100);
		String email = requiredText(form, "email", 1, 254);
		if (!EMAIL_PATTERN.matcher(email).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
		}
		String phoneNumber = requiredText(form, "phoneNumber", 6, 20);
		String recommendation = requiredText(form, "recommendation", 2, 100);
		String message = optionalText(form, "message", 1000);
		OffsetDateTime consentGivenAt = parseConsentTime(form.get("consentGivenAt"));
		int jobPositionId = parseJobPositionId(form.get("jobPositionId"));
		JobPosition jobPosition = jobPositionRepository.findById(jobPositionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job position not found"));
		Application existing = applicationRepository.findFirstByEmailIgnoreCase(email).orElse(null);

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

		if (existing != null && existing.getCvStoredName() != null) {
			deletePreviousCv(existing.getCvStoredName());
		}
		HttpStatus responseStatus = existing == null ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(responseStatus)
				.body(Map.of(
						"id", saved.getId(),
						"status", saved.getStatus(),
						"receivedAt", saved.getReceivedAt()));
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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consentGivenAt is required");
		}
		try {
			return OffsetDateTime.parse(text);
		} catch (java.time.format.DateTimeParseException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consentGivenAt", exception);
		}
	}

	private int parseJobPositionId(Object value) {
		try {
			int id = Integer.parseInt(String.valueOf(value));
			if (id > 0) {
				return id;
			}
		} catch (NumberFormatException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid jobPositionId", exception);
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid jobPositionId");
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
