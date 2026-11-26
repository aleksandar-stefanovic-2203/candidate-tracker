package com.manageitout.candidate_tracker.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.manageitout.candidate_tracker.dao.JobPositionRepository;
import com.manageitout.candidate_tracker.models.JobPosition;

class JobPositionControllerTest {

	private JobPositionRepository jobPositionRepository;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		jobPositionRepository = mock(JobPositionRepository.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new JobPositionController(jobPositionRepository))
				.build();
	}

	@Test
	void getPositionsWithoutActiveFilterReturnsAllPositions() throws Exception {
		JobPosition position = position(1, true);
		when(jobPositionRepository.findAll()).thenReturn(List.of(position));

		mockMvc.perform(get("/api/positions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].active").value(true));

		verify(jobPositionRepository).findAll();
		verifyNoMoreInteractions(jobPositionRepository);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void getPositionsWithActiveFilterReturnsMatchingPositions(boolean active) throws Exception {
		JobPosition position = position(2, active);
		when(jobPositionRepository.findByActive(active)).thenReturn(List.of(position));

		mockMvc.perform(get("/api/positions").param("active", Boolean.toString(active)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].active").value(active));

		verify(jobPositionRepository).findByActive(active);
		verifyNoMoreInteractions(jobPositionRepository);
	}

	private JobPosition position(int id, boolean active) {
		JobPosition position = new JobPosition();
		position.setId(id);
		position.setTitle("Software Engineer");
		position.setActive(active);
		return position;
	}
}
