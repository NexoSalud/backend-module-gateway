package com.reactive.nexo.service;

import com.reactive.nexo.client.EmployeeClient;
import com.reactive.nexo.dto.LoginRequest;
import com.reactive.nexo.dto.LoginResponse;
import com.reactive.nexo.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class SessionService {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticate user by calling EmployeeClient to validate credentials and get roles/permisos
     */
    public Mono<LoginResponse> login(LoginRequest request) {
        log.info("SessionService.login - Attempting login for user with identification: {}/{}", 
                request.getIdentification_type(), request.getIdentification_number());

        // Forward authentication request to employees module (or use mock if enabled)
        return employeeClient.authenticate(request)
                .flatMap(authResponse -> {
                    // Generate JWT token with user info and permissions
                    String token = jwtUtil.generateToken(
                            authResponse.getId(),
                            authResponse.getNames() + " " + authResponse.getLastnames(),
                            authResponse.getRol_nombre(),
                            authResponse.getPermisos()
                    );

                    LoginResponse response = new LoginResponse(
                            token,
                            authResponse.getNames() + " " + authResponse.getLastnames(),
                            authResponse.getId(),
                            authResponse.getRol_nombre()
                    );

                    log.info("SessionService.login - Login successful for user id: {}", authResponse.getId());
                    return Mono.just(response);
                });
    }
}
