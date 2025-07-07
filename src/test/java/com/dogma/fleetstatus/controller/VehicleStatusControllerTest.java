package com.dogma.fleetstatus.controller;

import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.service.VehicleStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleStatusController.class)
class VehicleStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleStatusService vehicleStatusService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public VehicleStatusService vehicleStatusService() {
            return org.mockito.Mockito.mock(VehicleStatusService.class);
        }
    }

    @Test
    void testGetVehicleStatus_Success() throws Exception {
        // Подготовка - мокируем поведение сервиса
        VehicleStatus vehicleStatus = new VehicleStatus();
        vehicleStatus.setVin("TESTVIN123");
        vehicleStatus.setOdometer(1000L);
        vehicleStatus.setTimestamp(LocalDateTime.now());

        when(vehicleStatusService.getLatestStatus("TESTVIN123"))
                .thenReturn(Optional.of(vehicleStatus));

        // Выполнение и проверка
        mockMvc.perform(get("/api/vehicle/TESTVIN123/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value("TESTVIN123"))
                .andExpect(jsonPath("$.odometer").value(1000L));
    }

    @Test
    void testGetVehicleStatus_NotFound() throws Exception {
        // Подготовка - мокируем пустой результат
        when(vehicleStatusService.getLatestStatus("UNKNOWN"))
                .thenReturn(Optional.empty());

        // Выполнение и проверка
        mockMvc.perform(get("/api/vehicle/UNKNOWN/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
