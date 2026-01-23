package com.reactive.nexo.security;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionChecker {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    /**
     * Verifica si un método y path dados tienen permiso en la lista.
     *
     * @param requestMethod El método HTTP de la solicitud (ej. "POST", "GET").
     * @param requestPath   La ruta de la solicitud (ej. "/api/v1/employees/7").
     * @param permissions list permissions
     * @return true si se encuentra un permiso coincidente, false en caso contrario.
     */
    public boolean hasPermission(String requestMethod, String requestPath,List<Map<String, Object>> permissions) {
        String normalizedPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
        normalizedPath = normalizedPath.endsWith("/") ? normalizedPath.substring(0, normalizedPath.length() - 1) : normalizedPath;
        //normalizedPath = normalizedPath.contains("?") ? normalizedPath.substring(0, normalizedPath.indexOf("?")) : normalizedPath;
        
        if(normalizedPath.startsWith("/api/v1/2fa/")){
            return true;
        }
        if (permissions != null && !permissions.isEmpty()) {
            logger.info("Iniciando impresión detallada de permisos ({} elementos):", permissions.size());
            
            for (Map<String, Object> permissionRule : permissions) {
                @SuppressWarnings("unchecked")           
                List<String> endpoints = (List<String>) permissionRule.get(requestMethod);
                logger.info("Regla de permiso: {}", permissionRule.get(requestMethod));
                if(endpoints !=  null){
                      for (String allowedPattern : endpoints) {             
                         allowedPattern = allowedPattern.endsWith("/") ?
                             allowedPattern.substring(0, allowedPattern.length() - 1) : allowedPattern;  
                         allowedPattern = allowedPattern.startsWith("/") ? allowedPattern : "/" + allowedPattern;
                         // remove placeholders enclosed in {}
                         allowedPattern = allowedPattern.replaceAll("\\{[^}]*\\}", "");
                         // normalize multiple slashes and trailing slash
                         allowedPattern = allowedPattern.replaceAll("/{2,}", "/");
                         allowedPattern = allowedPattern.endsWith("/") && !"/".equals(allowedPattern)
                             ? allowedPattern.substring(0, allowedPattern.length() - 1)
                             : allowedPattern;
                       logger.info("Regla de permiso: {}", allowedPattern +":"+normalizedPath);
                       if(normalizedPath.startsWith(allowedPattern)){
                        return true;
                       }
                    }
                }
            }
            logger.info("Impresión de permisos finalizada.");
        } else {
            logger.info("La lista de permisos está vacía o es nula.");
        }
        

        return false;
    }
}