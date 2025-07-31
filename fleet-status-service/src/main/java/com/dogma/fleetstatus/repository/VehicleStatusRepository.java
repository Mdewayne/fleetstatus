package com.dogma.fleetstatus.repository;

import com.dogma.fleetstatus.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с данными статуса транспортных средств
 * 
 * Предоставляет методы для поиска и сохранения информации о ТС
 * с использованием Spring Data JPA
 */
@Repository
public interface VehicleStatusRepository extends JpaRepository<VehicleStatus, Long> {
    
    /**
     * Находит последний статус ТС по VIN
     * 
     * @param vin уникальный идентификатор ТС
     * @return Optional с последним статусом или пустой Optional если ТС не найдено
     */
    Optional<VehicleStatus> findTopByVinOrderByTimestampDesc(String vin);
    
    /**
     * Находит все статусы ТС за указанный период времени
     * 
     * @param vin уникальный идентификатор ТС
     * @param startTime начало периода
     * @param endTime конец периода
     * @return список статусов за период
     */
    @Query("SELECT vs FROM VehicleStatus vs WHERE vs.vin = :vin AND vs.timestamp BETWEEN :startTime AND :endTime ORDER BY vs.timestamp DESC")
    List<VehicleStatus> findByVinAndTimestampBetween(@Param("vin") String vin, 
                                                     @Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * Проверяет существование ТС с указанным VIN
     * 
     * @param vin уникальный идентификатор ТС
     * @return true если ТС существует, false в противном случае
     */
    boolean existsByVin(String vin);
}

