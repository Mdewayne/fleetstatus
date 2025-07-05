package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.VehicleStatusRequest;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.repository.VehicleStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleStatusServiceTest {

    @Mock
    private VehicleStatusRepository repository;

    @InjectMocks
    private VehicleStatusService service;

    @Test
    void shouldSaveVehicleStatus() {
        // DTO с входными данными
        VehicleStatusRequest request = new VehicleStatusRequest();
        request.setVin("VIN123");
        request.setOdometer(1000L);

        // Ожидаемый результат
        VehicleStatus entity = new VehicleStatus();
        entity.setVin(request.getVin());
        entity.setOdometer(request.getOdometer());
        entity.setTimestamp(LocalDateTime.now());

        // Заглушка репозитория
        when(repository.save(any(VehicleStatus.class))).thenReturn(entity);

        // Запускаем сервис
        VehicleStatus saved = service.saveStatus(request);

        assertEquals("VIN123", saved.getVin());
        assertEquals(1000L, saved.getOdometer());

        // Проверка, что репозиторий вызвался
        verify(repository, times(1)).save(any(VehicleStatus.class));
    }

    @Test
    void testGetLatestStatus_found() {
        VehicleStatus vs = new VehicleStatus();
        vs.setVin("VIN123");
        vs.setOdometer(1000L);
        vs.setTimestamp(LocalDateTime.now());

        when(repository.findTopByVinOrderByTimestampDesc("VIN123"))
                .thenReturn(Optional.of(vs));

        Optional<VehicleStatus> result = service.getLatestStatus("VIN123");
        assertTrue(result.isPresent());
        assertEquals("VIN123", result.get().getVin());
        assertEquals(1000, result.get().getOdometer());

        verify(repository, times(1)).findTopByVinOrderByTimestampDesc("VIN123");
    }

    @Test
    void testGetLatestStatus_notFound() {
        when(repository.findTopByVinOrderByTimestampDesc("UNKNOWN"))
                .thenReturn(Optional.empty());

        Optional<VehicleStatus> result = service.getLatestStatus("UNKNOWN");
        assertFalse(result.isPresent());

        verify(repository, times(1)).findTopByVinOrderByTimestampDesc("UNKNOWN");
    }
}
