package com.reactive.nexo.repository;

import com.reactive.nexo.model.Tracking;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface TrackingRepository extends ReactiveCrudRepository<Tracking, Long> {

    // Consultas para employee_id
    @Query("SELECT * FROM tracking WHERE employee_id = :value ORDER BY created_at DESC")
    Flux<Tracking> findByEmployeeIdEquals(@Param("value") Long value);

    @Query("SELECT * FROM tracking WHERE employee_id < :value ORDER BY created_at DESC")
    Flux<Tracking> findByEmployeeIdLessThan(@Param("value") Long value);

    @Query("SELECT * FROM tracking WHERE employee_id > :value ORDER BY created_at DESC")
    Flux<Tracking> findByEmployeeIdGreaterThan(@Param("value") Long value);

    // Consultas para action
    @Query("SELECT * FROM tracking WHERE action = :value ORDER BY created_at DESC")
    Flux<Tracking> findByActionEquals(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE action < :value ORDER BY created_at DESC")
    Flux<Tracking> findByActionLessThan(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE action > :value ORDER BY created_at DESC")
    Flux<Tracking> findByActionGreaterThan(@Param("value") String value);

    // Consultas para created_at (requiere conversión de String a LocalDateTime)
    @Query("SELECT * FROM tracking WHERE created_at = :value ORDER BY created_at DESC")
    Flux<Tracking> findByCreatedAtEquals(@Param("value") LocalDateTime value);

    @Query("SELECT * FROM tracking WHERE created_at < :value ORDER BY created_at DESC")
    Flux<Tracking> findByCreatedAtLessThan(@Param("value") LocalDateTime value);

    @Query("SELECT * FROM tracking WHERE created_at > :value ORDER BY created_at DESC")
    Flux<Tracking> findByCreatedAtGreaterThan(@Param("value") LocalDateTime value);

    // Consultas para id
    @Query("SELECT * FROM tracking WHERE id = :value ORDER BY created_at DESC")
    Flux<Tracking> findByIdEquals(@Param("value") Long value);

    @Query("SELECT * FROM tracking WHERE id < :value ORDER BY created_at DESC")
    Flux<Tracking> findByIdLessThan(@Param("value") Long value);

    @Query("SELECT * FROM tracking WHERE id > :value ORDER BY created_at DESC")
    Flux<Tracking> findByIdGreaterThan(@Param("value") Long value);

    // Consultas para data (búsqueda de texto)
    @Query("SELECT * FROM tracking WHERE data = :value ORDER BY created_at DESC")
    Flux<Tracking> findByDataEquals(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE data < :value ORDER BY created_at DESC")
    Flux<Tracking> findByDataLessThan(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE data > :value ORDER BY created_at DESC")
    Flux<Tracking> findByDataGreaterThan(@Param("value") String value);

    // Consultas para result (búsqueda de texto)
    @Query("SELECT * FROM tracking WHERE result = :value ORDER BY created_at DESC")
    Flux<Tracking> findByResultEquals(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE result < :value ORDER BY created_at DESC")
    Flux<Tracking> findByResultLessThan(@Param("value") String value);

    @Query("SELECT * FROM tracking WHERE result > :value ORDER BY created_at DESC")
    Flux<Tracking> findByResultGreaterThan(@Param("value") String value);

    // Método adicional para obtener los últimos registros
    @Query("SELECT * FROM tracking ORDER BY created_at DESC LIMIT :limit")
    Flux<Tracking> findLatestTracking(@Param("limit") int limit);
}