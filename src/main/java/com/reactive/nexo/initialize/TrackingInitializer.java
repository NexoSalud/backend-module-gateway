package com.reactive.nexo.initialize;

import com.reactive.nexo.model.Tracking;
import com.reactive.nexo.repository.TrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class TrackingInitializer implements CommandLineRunner {

    private final TrackingRepository trackingRepository;

    @Override
    public void run(String... args) throws Exception {
        trackingRepository.count()
                .flatMapMany(count -> {
                    if (count == 0) {
                        log.info("Inicializando datos de tracking...");
                        return createSampleTracking();
                    } else {
                        log.info("Los datos de tracking ya existen, omitiendo inicialización");
                        return Flux.empty();
                    }
                })
                .doOnComplete(() -> log.info("Inicialización de tracking completada"))
                .subscribe();
    }

    private Flux<Tracking> createSampleTracking() {
        List<Tracking> trackingList = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        // Ejemplos de llamadas a endpoints
        trackingList.add(new Tracking(
                1L,
                "endpoint called: /auth/login",
                "{\"username\": \"admin\", \"password\": \"***\"}",
                "{\"status\": \"success\", \"token\": \"jwt123...\"}"
        ).withCreatedAt(baseTime.plusHours(1)));

        trackingList.add(new Tracking(
                2L,
                "endpoint called: /api/v1/schedule",
                "{\"employeeId\": 1, \"userId\": 100, \"startAt\": \"2024-12-02T09:00:00\"}",
                "{\"status\": \"created\", \"id\": 1}"
        ).withCreatedAt(baseTime.plusHours(2)));

        // Ejemplos de autenticación
        trackingList.add(new Tracking(
                1L,
                "auth: login_attempt",
                "{\"username\": \"admin\", \"ip\": \"192.168.1.100\"}",
                "{\"status\": \"success\"}"
        ).withCreatedAt(baseTime.plusHours(3)));

        trackingList.add(new Tracking(
                3L,
                "auth: logout",
                "{\"session_id\": \"sess123\"}",
                "{\"status\": \"success\"}"
        ).withCreatedAt(baseTime.plusHours(4)));

        // Ejemplos de errores
        trackingList.add(new Tracking(
                null,
                "error: validation_error",
                "{\"field\": \"email\", \"message\": \"Invalid format\"}",
                "ValidationException: Email format is invalid"
        ).withCreatedAt(baseTime.plusHours(5)));

        // Más llamadas a endpoints
        trackingList.add(new Tracking(
                1L,
                "endpoint called: /api/v1/users",
                "{}",
                "{\"data\": [...], \"total\": 25}"
        ).withCreatedAt(baseTime.plusHours(6)));

        trackingList.add(new Tracking(
                2L,
                "endpoint called: /api/v1/employees",
                "{\"page\": 0, \"size\": 10}",
                "{\"content\": [...], \"totalElements\": 15}"
        ).withCreatedAt(baseTime.plusHours(7)));

        // Acciones del sistema
        trackingList.add(new Tracking(
                null,
                "system: startup",
                "{\"module\": \"gateway\", \"port\": 8080}",
                "{\"status\": \"started\", \"uptime\": 0}"
        ).withCreatedAt(baseTime.plusHours(8)));

        trackingList.add(new Tracking(
                4L,
                "endpoint called: /tracking/latest",
                "{\"limit\": 50}",
                "{\"count\": 8}"
        ).withCreatedAt(baseTime.plusHours(9)));

        // Acciones de sesiones grupales (del nuevo módulo schedule)
        trackingList.add(new Tracking(
                1L,
                "endpoint called: /api/v1/schedule",
                "{\"employeeId\": 1, \"userId\": 101, \"groupSession\": true, \"startAt\": \"2024-12-02T16:00:00\", \"endAt\": \"2024-12-02T17:00:00\"}",
                "{\"status\": \"created\", \"id\": 15, \"groupSession\": true}"
        ).withCreatedAt(baseTime.plusHours(10)));

        return trackingRepository.saveAll(trackingList);
    }
}