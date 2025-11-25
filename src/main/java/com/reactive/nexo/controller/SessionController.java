package com.reactive.nexo.controller;

import org.springframework.web.server.ServerWebExchange;
import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.LoginResponse;
import com.reactive.nexo.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.reactive.nexo.util.TwoFactorUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/auth")
@Slf4j
public class SessionController {

    @Autowired
    private SessionService sessionService;
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    /**
     * POST /api/v1/auth/login - Login endpoint
     * Accepts identification_type, identification_number, and password
     * Returns JWT token if successful
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request, ServerWebExchange exchange) {
        log.info("SessionController.login - Login request for user: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());
        
        return sessionService.login(request,exchange)
                .map(response -> ResponseEntity.status(HttpStatus.OK).body(response))
                .onErrorResume(err -> {
                    log.error("SessionController.login - Login failed: {}", err.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    // Asume que tienes acceso al ID/email del usuario autenticado actual
    // En un entorno real, obtendrías esto del contexto de seguridad (e.g., Spring Security Context)
    private final String APP_ISSUER = "NexoReactiveApp";
    

    /**
     * Endpoint para generar y devolver la imagen del código QR para la configuración 2FA.
     */
    @GetMapping(value = "/2fa/generate-qr/{identificationType}/{identificationNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<ResponseEntity<byte[]>> generateQrCode(@PathVariable String identificationType, @PathVariable String identificationNumber,ServerWebExchange exchange) {
        
        // 1. Generar un nuevo secreto para el usuario
        String newSecret = TwoFactorUtil.generateNewSecret();
        logger.info("SessionController.generateQrCode - Generated new 2FA secret for user: {}/{} {}", identificationType, identificationNumber,newSecret);
        
        // 2. Guardar este secreto en la base de datos para el usuario actual (MUY IMPORTANTE)
        if(!sessionService.saveTwoFactorSecret(exchange,identificationType, identificationNumber, newSecret).block()) {
            logger.error("SessionController.generateQrCode - Failed to save 2FA secret for user: {}/{}", identificationType, identificationNumber);
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }

        // 3. Generar la URL del QR
        String qrUrl = TwoFactorUtil.getQRUrl(identificationType+"@"+identificationNumber, newSecret, APP_ISSUER);

        return Mono.fromCallable(() -> {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrUrl, BarcodeFormat.QR_CODE, 200, 200);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(pngData);
        }).onErrorResume(e -> {
            // Manejar errores de generación de QR
            return Mono.just(ResponseEntity.internalServerError().body(null));
        });
    }

    /**
     * POST /api/v1/auth/logout - Logout endpoint (stateless, no action needed on server)
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {
        log.info("SessionController.logout - Logout request");
        return Mono.just(ResponseEntity.ok().<Void>build());
    }
}

