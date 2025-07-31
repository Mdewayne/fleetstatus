package com.dogma.fleetstatus.exception;

/**
 * Исключение, возникающее при попытке получить данные о ТС,
 * которое не существует в системе
 */
public class VehicleNotFoundException extends RuntimeException {
    
    public VehicleNotFoundException(String vin) {
        super(String.format("Vehicle with VIN %s not found", vin));
    }
    
    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 