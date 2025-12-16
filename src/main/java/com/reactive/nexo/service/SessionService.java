package com.reactive.nexo.service;

import org.springframework.web.server.ServerWebExchange;
import com.reactive.nexo.client.EmployeeClient;
import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.LoginResponse;
import com.reactive.nexo.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;
import com.reactive.nexo.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.reactive.nexo.util.TwoFactorUtil;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
@Slf4j
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticate user by calling EmployeeClient to validate credentials and get roles/permisos
     */
    @SuppressWarnings("unchecked")           
    public Mono<LoginResponse> login(LoginRequest request, ServerWebExchange exchange) {
        log.info("SessionService.login - Attempting login for user with identification: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());

        // Forward authentication request to employees module (or use mock if enabled)
        return employeeClient.authenticate(request)
                .flatMap(authResponse -> {
                    // Generate JWT token with user info and permissions

                    String employeeId = authResponse.getId().toString();
                    String userAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);                    
                    String ipAddress =  (exchange.getRequest().getRemoteAddress() != null) ? 
                          exchange.getRequest().getRemoteAddress().getHostString() : "unknown";
                    
                    String redirectUrl = null;                
                    logger.info("SessionService.readTwoFactorSecret - Fetching employee to update 2FA secret for user: {}/{}",request.getTwoFA(), employeeId);
       
                    if (request.getTwoFA() == null || request.getTwoFA().isEmpty()) {
                        logger.info("SessionService.saveTwoFactorSecret - Fetching employee to update 2FA secret for user: {}/{}",request.getTwoFA(), employeeId);
                       // return Mono.<LoginResponse>error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "2FA code is required"));
                       // return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));        
                    } 
                
                    // *** Validación real del código 2FA usando el secreto del usuario ***
                    logger.info("SessionService.saveTwoFactorSecret - Fetching employee to update 2FA secret for user: {}/{}",request.getTwoFA(), authResponse.getSecret());
                    if(authResponse.getSecret() != null) {
                        if (!TwoFactorUtil.validateCode(authResponse.getSecret(), request.getTwoFA())) {    
                            logger.info("SessionService.saveTwoFactorSecret - No 2FA secret is INVALID");
                            return Mono.<LoginResponse>error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "2FA code is required"));
                        } else {
                            logger.info("SessionService.saveTwoFactorSecret - No 2FA secret VALID");      
                        }
                    } else {
                        logger.info("SessionService.saveTwoFactorSecret - No 2FA secret UNSETED");                        
                        redirectUrl = String.format("/api/v1/2fa/generate-qr/%s/%s", 
                            request.getIdentification_type(), request.getIdentification_number());
                            exchange.getResponse().setStatusCode(HttpStatus.OK);
                        authResponse.setPermissions(null); 
                    }

                    String token = jwtUtil.generateToken(
                            authResponse.getId(),
                            ipAddress,
                            userAgent,
                            authResponse.getPermissions()
                    );

                    LoginResponse response = new LoginResponse(
                            token,
                            authResponse.getNames() + " " + authResponse.getLastnames(),
                            authResponse.getId(),
                            authResponse.getRol_name(),
                            authResponse.getPermissions()
                    );

                    if(redirectUrl != null) {
                        Map<String, Object> responseBody = Map.of(
                            "redirect_url", redirectUrl,
                            "token", token,
                            "employeeId", authResponse.getId().toString()
                        );
                        // Complete the response so headers are sent
                        return Mono.just(responseBody)
                            .flatMap(body -> {
                                exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                                try {
                                    byte[] bytes = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(body);
                                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                        .bufferFactory().wrap(bytes)));
                                } catch (Exception e) {
                                    logger.error("SessionService.login - Error writing redirect response: {}", e.getMessage());
                                    return exchange.getResponse().setComplete();
                                }
                            })
                            .then(Mono.empty());
                    }
                    log.info("SessionService.login - Login successful for user id: {}", authResponse.getId());
                    return Mono.just(response);
                });
    }

    public Mono<Boolean> saveTwoFactorSecret(ServerWebExchange exchange, String identificationType, String identificationNumber, String newSecret) {
        logger.info("SessionService.saveTwoFactorSecret - Fetching employee to update 2FA secret for user: {}/{}", identificationType, identificationNumber);
        
        return employeeClient.getEmployee(identificationType, identificationNumber)
            .flatMap(response -> {
                logger.info("SessionService.saveTwoFactorSecret - Fetched employee ID from response: {}", response.getId());

                String employeeId = exchange.getRequest().getHeaders().getFirst("x-employee-id");
                logger.info("SessionService.saveTwoFactorSecret - Fetched employee: {}", employeeId);
                if(response != null && response.getId().equals(employeeId)) {
                    return employeeClient.updateTwoFactorSecret(employeeId, newSecret);
                }
                return Mono.just(true); 
            })
            .doOnError(err -> {
                logger.error("SessionService.saveTwoFactorSecret - Error in client call: {}", err.getMessage());
            })
            .onErrorReturn(false); 
    }
}
