package com.dogma.fleetstatus.service;

import com.dogma.fleetstatus.dto.VehicleStatusRequest;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.repository.VehicleStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VehicleStatusService {
    private final VehicleStatusRepository repository;

    public VehicleStatusService(VehicleStatusRepository repository) {
        this.repository = repository;
    }

    public Optional<VehicleStatus> getLatestStatus(String vin) {
        return repository.findTopByVinOrderByTimestampDesc(vin);
    }

    public VehicleStatus saveStatus(VehicleStatusRequest request) {
        VehicleStatus status = new VehicleStatus();
        status.setVin(request.getVin());
        status.setOdometer(request.getOdometer());
        status.setTimestamp(LocalDateTime.now());
        return repository.save(status);
    }
}

