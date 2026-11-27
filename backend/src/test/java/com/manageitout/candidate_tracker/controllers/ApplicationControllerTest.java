package com.manageitout.candidate_tracker.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import com.manageitout.candidate_tracker.dao.ApplicationRepository;
import com.manageitout.candidate_tracker.dao.JobPositionRepository;
import com.manageitout.candidate_tracker.models.Application;
import com.manageitout.candidate_tracker.models.ApplicationStatus;
import com.manageitout.candidate_tracker.models.JobPosition;

class ApplicationControllerTest {

	private static final byte[] PDF_CONTENT = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
	private static final String EMAIL = "alex@example.com";
	private static final String CONSENT_TIME = "2026-03-14T10:15:30Z";

	private ApplicationRepository applicationRepository;
	private JobPositionRepository jobPositionRepository;
	private MockMvc mockMvc;
	private final List<String> uploadedCvNames = new CopyOnWriteArrayList<>();
	private final AtomicInteger nextApplicationId = new AtomicInteger(100);

	@BeforeEach
	void setUp() {
		applicationRepository = mock(ApplicationRepository.class, CALLS_REAL_METHODS);
		jobPositionRepository = mock(JobPositionRepository.class);
		doReturn(Optional.empty()).when(applicationRepository).findFirstByEmailIgnoreCase(anyString());
		doAnswer(invocation -> {
			Application application = invocation.getArgument(0);
			if (application.getId() == null) {
				application.setId(nextApplicationId.incrementAndGet());
			}
			uploadedCvNames.add(application.getCvStoredName());
			return application;
		}).when(applicationRepository).save(any(Application.class));

		mockMvc = MockMvcBuilders
				.standaloneSetup(new ApplicationController(applicationRepository, jobPositionRepository))
				.build();
	}

	@AfterEach
	void removeUploadedTestFiles() throws IOException {
		Path uploadDirectory = Path.of("uploads").toAbsolutePath().normalize();
		for (String filename : uploadedCvNames) {
			Files.deleteIfExists(uploadDirectory.resolve(filename));
		}
	}

	@Test
	void getApplicationsReturnsAllApplications() throws Exception {
		Application application = new Application();
		application.setId(7);
		application.setName("Alex Candidate");
		when(applicationRepository.findAll()).thenReturn(List.of(application));

		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(7))
				.andExpect(jsonPath("$[0].name").value("Alex Candidate"));

		verify(applicationRepository).findAll();
		verifyNoInteractions(jobPositionRepository);
	}

	@Test
	void submitApplicationCreatesFirstApplication() throws Exception {
		submit(validApplication(), pdfFile())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(101))
				.andExpect(jsonPath("$.status").value(ApplicationStatus.NEW.name()));

		verify(applicationRepository).save(any(Application.class));
	}

	@Test
	void submitApplicationWithInvalidFieldReturnsBadRequest() throws Exception {
		Map<String, Object> form = validApplication();
		form.put("email", "not-an-email");

		submit(form, pdfFile()).andExpect(status().isBadRequest());

		verifyNoInteractions(applicationRepository);
		verifyNoInteractions(jobPositionRepository);
	}

	@Test
	void submitApplicationForMissingPositionReturnsNotFound() throws Exception {
		Map<String, Object> form = validApplication();
		form.put("job_position_id", 999);
		when(jobPositionRepository.findById(999)).thenReturn(Optional.empty());

		submit(form, pdfFile())
				.andExpect(status().isNotFound())
				.andExpect(result -> assertThat(result.getResolvedException())
						.isInstanceOfSatisfying(
								ResponseStatusException.class,
								exception -> assertThat(exception.getReason()).isEqualTo("Job position not found")));

		verifyNoInteractions(applicationRepository);
	}

	@Test
	void submitApplicationForInactivePositionReturnsNotFound() throws Exception {
		Map<String, Object> form = validApplication();
		form.put("job_position_id", 2);
		when(jobPositionRepository.findById(2)).thenReturn(Optional.of(position(2, false)));

		submit(form, pdfFile())
				.andExpect(status().isNotFound())
				.andExpect(result -> assertThat(result.getResolvedException())
						.isInstanceOfSatisfying(
								ResponseStatusException.class,
								exception -> assertThat(exception.getReason()).isEqualTo("Job position is inactive")));

		verifyNoInteractions(applicationRepository);
	}

	@Test
	void submitApplicationWithOversizedCvReturnsContentTooLarge() throws Exception {
		byte[] oversizedPdf = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(PDF_CONTENT, 0, oversizedPdf, 0, PDF_CONTENT.length);
		MockMultipartFile cv = new MockMultipartFile(
				"cv",
				"resume.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				oversizedPdf);

		submit(validApplication(), cv).andExpect(status().isContentTooLarge());
	}

	@Test
	void submitApplicationWithNonPdfCvReturnsUnsupportedMediaType() throws Exception {
		MockMultipartFile cv = new MockMultipartFile(
				"cv",
				"resume.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"not a PDF".getBytes(StandardCharsets.UTF_8));

		submit(validApplication(), cv).andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void submitApplicationAgainUpdatesExistingApplication() throws Exception {
		Application existing = new Application();
		existing.setId(12);
		existing.setEmail(EMAIL);
		existing.setCvStoredName("previous-" + UUID.randomUUID() + ".pdf");
		existing.setStatus(ApplicationStatus.NEW);
		existing.setReceivedAt(OffsetDateTime.parse(CONSENT_TIME));
		doReturn(Optional.of(existing)).when(applicationRepository).findFirstByEmailIgnoreCase(EMAIL);
		Map<String, Object> form = validApplication();
		form.put("name", "Alex Updated");

		submit(form, pdfFile())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.status").value(ApplicationStatus.NEW.name()));

		assertThat(existing.getName()).isEqualTo("Alex Updated");
		verify(applicationRepository).save(any(Application.class));
	}

	private ResultActions submit(Map<String, Object> form, MockMultipartFile cv) throws Exception {
		byte[] applicationJson = JsonMapper.builder().build().writeValueAsBytes(form);
		MockMultipartFile applicationPart = new MockMultipartFile(
				"application",
				"",
				MediaType.APPLICATION_JSON_VALUE,
				applicationJson);
		return mockMvc.perform(multipart("/api/applications")
				.file(applicationPart)
				.file(cv)
				.accept(MediaType.APPLICATION_JSON));
	}

	private Map<String, Object> validApplication() {
		return new HashMap<>(Map.of(
				"name", "Alex Candidate",
				"email", EMAIL,
				"phone_number", "+381601234567",
				"recommendation", "LinkedIn",
				"consent_given_at", CONSENT_TIME));
	}

	private MockMultipartFile pdfFile() {
		return new MockMultipartFile(
				"cv",
				"resume.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				PDF_CONTENT);
	}

	private JobPosition position(int id, boolean active) {
		JobPosition position = new JobPosition();
		position.setId(id);
		position.setTitle("Software Engineer");
		position.setActive(active);
		return position;
	}
}
