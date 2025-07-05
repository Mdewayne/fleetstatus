package com.dogma.fleetstatus.controller;

import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.repository.VehicleStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class VehicleStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleStatusRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        VehicleStatus vs = new VehicleStatus();
        vs.setVin("TESTVIN123");
        vs.setOdometer(1000L);
        vs.setTimestamp(LocalDateTime.now());
        repository.save(vs);
    }

    @Test
    void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/vehicle/TESTVIN123/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value("TESTVIN123"))
                .andExpect(jsonPath("$.odometer").value(1000L));
    }
}
