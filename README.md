# reactive-nexo — Backend [API reactiva con Spring WebFlux y R2DBC]

Proyecto NEXO-Hospital modulos Usuarios/Pacientes [API reactiva con Spring WebFlux y R2DBC].

Estado actual del proyecto
- Paquete raíz de la aplicación: `com.reactive.nexo`.
- Stack: Spring Boot 3.x, Spring WebFlux, Spring Data R2DBC.
- Bases de datos: soporta R2DBC (Postgres u otros drivers). Para pruebas y desarrollo ligero el proyecto usa H2 en memoria (configurable en `application.yml`).

Requisitos mínimos
- Java 17 o superior (en este repositorio se ha usado Java 19 en pruebas).
- Maven 3.6+

El proyecto ya incluye springdoc (starter WebFlux UI), por lo que la documentación OpenAPI/Swagger queda disponible en las rutas por defecto. 
En tu entorno local (ejecutando la app en el puerto por defecto 8080) puedes encontrarla en:

Interfaz web (Swagger UI):

http://localhost:8080/swagger-ui.html
o (equivalente) http://localhost:8080/swagger-ui/index.html
Documento OpenAPI (JSON):

http://localhost:8080/v3/api-docs
Documento OpenAPI (YAML):

http://localhost:8080/v3/api-docs.yaml

Cómo compilar y ejecutar


1. Cargar base de datos:
```bash
mvn flyway:migrate
```

2. Compilar y ejecutar tests:

```bash
mvn clean test
```

3. Empaquetar la aplicación:

```bash
mvn clean package
```

4. Ejecutar la JAR producida:

```bash
java -jar target/reactive-nexo-0.0.1-SNAPSHOT.jar
```

Nota: la configuración de conexión a BD se controla desde `src/main/resources/application.yml`. Cambia el perfil o las propiedades para usar Postgres u otro R2DBC driver.

Qué hace esta versión
- Endpoints CRUD para usuarios en `/users` (reactivo).
- Endpoint extendido: GET `/api/v1/users/{userId}` ahora devuelve, además de los datos del usuario, todos los atributos asociados y sus valores.
  - La representación es un DTO con la siguiente forma:

```json
{
  "id": 1,
  "name": "Juan Campo",
  "identification_type": "CC",
  "identification_number": "745454654",
  "attributes": [
    { "attribute_name": "fecha de nacimiento", "values": ["1992-05-06"] },
    { "attribute_name": "lugar de nacimiento ciudad", "values": ["cali"] },
    ...
  ]
}
```

Datos de ejemplo (seed)
- Al iniciar con perfil distinto a `test`, el `UserInitializer` inserta usuarios de ejemplo y crea atributos y valores para cada usuario. Entre los atributos/valores insertados están:
  - fecha de nacimiento: `1992-05-06`
  - lugar de nacimiento ciudad: `cali`
  - lugar de nacimiento departamento: `valle`
  - lugar de nacimiento pais: `colombia`
  - ubicacion ciudad: `guachene`
  - ubicacion departamento: `cauca`
  - ubicacion pais: `colombia`
  - entidad de salud: `sura`
  - ultima consulta: `2024-06-06`
  - telefono: `315-000-0000`
  - email: `jhon-doe@test.co`
  - regimen: `subcidiado`
  - atributos ejemplo de historia clínica: `historia_clinica_numero`, `diagnostico_principal`, `alergias` (con valores de ejemplo)

Endpoints principales

- GET /users — lista todos los usuarios (Flux)
- POST /users — crea un usuario (Mono)
- GET /users/{userId} — devuelve user + atributos con valores (Mono<UserWithAttributesDTO>)
- PUT /users/{userId} — actualiza usuario
- DELETE /users/{userId} — elimina usuario
- GET /users/events — stream (SSE) de usuarios

- GET /users/by-identification/{identificationType}/{identificationNumber} — busca un usuario por tipo y número de identificación y devuelve el usuario más todos los atributos asociados y sus valores (Mono<UserWithAttributesDTO>). Ejemplo:

  - Solicitud (curl):

    ```bash
    curl -s http://localhost:8080/api/v1/users/by-identification/CC/1 | jq .
    ```

  - Respuesta (ejemplo):

    ```json
    {
      "id": 1,
      "name": "Juan Campo",
      "identification_type": "CC",
      "identification_number": "745454654",
      "attributes": [

POST / PUT — crear y actualizar usuario con atributos
--------------------------------------------------

Los endpoints `POST /api/v1/users` y `PUT /api/v1/users/{id}` aceptan un JSON con los datos del usuario y un objeto `attributes` donde cada clave es el nombre del atributo y el valor es una lista de strings con los valores asociados.

Ejemplo de solicitud para crear/actualizar un usuario (con atributos):

```json
{
  "names": "Jhonn",
  "lastnames": "Campo",
  "identification_type": "CC",
  "identification_number": "858585",
  "attributes": {
    "fecha de nacimiento": ["1992-05-06"],
    "lugar de nacimiento ciudad": ["cali"],
    "telefono": ["315-000-0000"]
  }
}
```

Ejemplo (curl) para crear:

```bash
curl -s -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"names":"Jhonn","lastnames":"Campo","identification_type":"CC","identification_number":"858585","attributes":{"fecha de nacimiento":["1992-05-06"]}}' | jq .
```

Comportamiento importante:
- `attributes` es un mapa: llave = nombre del atributo, valor = lista de strings.
- Si un atributo existe y `multiple == false`, durante una actualización se reemplazan los valores previos por los nuevos (semántica de reemplazo).
- Las combinaciones `(identification_type, identification_number)` están protegidas por una constraint única; intentar crear/actualizar a una tupla ya usada por otro usuario devolverá 409 Conflict.
- Los nombres de atributos para un mismo usuario son únicos; la implementación utiliza un upsert a nivel SQL para evitar condiciones de carrera al crear/actualizar atributos.

Puedes usar `PUT /api/v1/users/{id}` con el mismo cuerpo JSON para reemplazar valores/añadir nuevos atributos.

        { "attribute_name": "fecha de nacimiento", "values": ["1992-05-06"] },
        { "attribute_name": "lugar de nacimiento ciudad", "values": ["cali"] },
        { "attribute_name": "telefono", "values": ["315-000-0000"] }
      ]
    }
    ```

Ver y probar el endpoint `/api/v1//users/{userId}`

1. Levanta la aplicación (ver pasos arriba).
2. Llama al endpoint (ejemplo con curl):

```bash
curl -s http://localhost:8080/api/v1/users/1 | jq .
```

Pruebas y desarrollo
- Los tests del proyecto están configurados para ejecutarse con H2 (perfil `test`). Para ejecutar la suite de pruebas:

```bash
mvn test
```

Si quieres cambiar a Postgres durante el desarrollo, actualiza `application.yml` o utiliza un perfil específico que apunte a tu instancia Postgres con el driver R2DBC apropiado.

Notas técnicas rápidas
- Modelos relevantes: `User`, `AttributeUser`, `ValueAttributeUser`.
- Repositorios reactivos: `UserRepository`, `AttributeUserRepository`, `ValueAttributeUserRepository`.
- Servicio que compone la respuesta con atributos/valores: `UserService.getUserWithAttributes(userId)`.
- Inicializador de datos: `UserInitializer` (inserta usuarios, atributos y valores de ejemplo).

Si quieres, añadimos un test de integración que verifique la estructura JSON devuelta por `/users/{userId}` y los valores semilla. Puedo implementarlo y ejecutarlo automáticamente.
