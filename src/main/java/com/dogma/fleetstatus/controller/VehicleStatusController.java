package com.dogma.fleetstatus.controller;

import com.dogma.fleetstatus.dto.VehicleStatusRequest;
import com.dogma.fleetstatus.entity.VehicleStatus;
import com.dogma.fleetstatus.service.VehicleStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicle")
public class VehicleStatusController {
    private final VehicleStatusService service;

    public VehicleStatusController(VehicleStatusService service) {
        this.service = service;
    }

    @GetMapping("/{vin}/status")
    public ResponseEntity<VehicleStatus> getStatus(@PathVariable String vin) {
        return service.getLatestStatus(vin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{vin}/status")
    public ResponseEntity<VehicleStatus> saveStatus(@PathVariable String vin,
                                                    @RequestBody VehicleStatusRequest request) {
        request.setVin(vin); // добавляем VIN из URL, если не передали в JSON
        VehicleStatus saved = service.saveStatus(request);
        return ResponseEntity.ok(saved);
    }
}
