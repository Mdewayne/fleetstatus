package com.dogma.fleetstatus.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_status")
@Data
public class VehicleStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vin;
    private Long odometer;
    private LocalDateTime timestamp;

}
