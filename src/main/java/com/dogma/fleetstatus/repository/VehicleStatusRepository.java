package com.dogma.fleetstatus.repository;

import com.dogma.fleetstatus.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleStatusRepository extends JpaRepository<VehicleStatus, Long> {
    Optional<VehicleStatus> findTopByVinOrderByTimestampDesc(String vin);
}

