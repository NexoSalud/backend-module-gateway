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
@RequestMapping("/2fa")
@Slf4j
public class TwoController {

    @Autowired
    private SessionService sessionService;
    private static final Logger logger = LoggerFactory.getLogger(TwoController.class);
   
    private final String APP_ISSUER = "Nexo Salud";
    

    /**
     * Endpoint para generar y devolver la imagen del código QR para la configuración 2FA.
     */
    @GetMapping(value = "/generate-qr/{identificationType}/{identificationNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<ResponseEntity<byte[]>> generateQrCode(@PathVariable String identificationType, @PathVariable String identificationNumber,ServerWebExchange exchange) {
        
        // 1. Generar un nuevo secreto para el usuario
        String newSecret = TwoFactorUtil.generateNewSecret();
        logger.info("TwoController.generateQrCode - Generated new 2FA secret for user: {}/{} {}", identificationType, identificationNumber,newSecret);
        
        // 2. Guardar este secreto en la base de datos para el usuario actual (MUY IMPORTANTE)
        return sessionService.saveTwoFactorSecret(exchange,identificationType, identificationNumber, newSecret).flatMap(success -> {
                if (success) {
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
                } else {
                    logger.error("TwoController.generateQrCode - Failed to save 2FA secret for user: {}/{}", identificationType, identificationNumber);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
                }
            });


       
    }
}

