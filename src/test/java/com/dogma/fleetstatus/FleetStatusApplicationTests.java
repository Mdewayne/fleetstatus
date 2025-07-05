package com.dogma.fleetstatus;

import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.repository.VehicleStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FleetStatusApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VehicleStatusRepository repository;

	@BeforeEach
	void setup() {
		repository.deleteAll();

		VehicleStatus vs = new VehicleStatus();
		vs.setVin("ABC123");
		vs.setOdometer(12345L);
		vs.setTimestamp(LocalDateTime.now());
		repository.save(vs);
	}

	@Test
	void testGetVehicleStatus() throws Exception {
		mockMvc.perform(get("/api/vehicle/ABC123/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vin").value("ABC123"))
				.andExpect(jsonPath("$.odometer").value(12345));
	}
}
