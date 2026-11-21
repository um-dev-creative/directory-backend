# Variables de Configuración Encontradas en bootstrap.yml

Este documento lista todas las variables de entorno y placeholders utilizados para configurar la aplicación `directory-backend`, agrupadas por áreas funcionales.

## 🔒 Seguridad y Autenticación (Security & Auth)
Variables cruciales para la gestión de secretos, tokens y los proveedores de identidad.

*   `APP_TOKEN_SECRET`: Secreto utilizado para el manejo de JWT (Bearer Tokens).
*   `APP_TOKEN_EXPIRATION`: Duración de vida del token en milisegundos.
*   `SSL_KEYSTORE_LOCATION`: Ruta del almacén de claves JKS.
*   `SSL_KEYSTORE_PASSWORD`: Contraseña del almacén de claves JKS.
*   `SSL_KEYSTORE_TYPE`: Tipo de almacén de claves (ej: 'JKS').
*   `SSL_TRUSTSTORE_LOCATION`: Ruta del almacén de confianza (Trust Store).
*   `SSL_TRUSTSTORE_PASSWORD`: Contraseña del Trust Store.
*   `SSL_TRUSTSTORE_TYPE`: Tipo de almacén de confianza.
*   **Backbone Integration:**
    *   `BACKBONE_GRANT_TYPE`: Tipo de concesión OAuth (ej: client credentials).
    *   `BACKBONE_SCOPE`: Alcance (scope) para Backbone.
    *   `BACKBONE_CLIENT_SECRET`: Secreto del cliente de Backbone.
    *   `BACKBONE_CLIENT_ID`: ID del cliente de Backbone.
    *   `AUTH_SERVER_URI`: URI base del servidor de autenticación OAuth. (Usado para el *redirect-uri*).
    *   `AUTH_TOKEN_URI`: URI específico para obtener tokens JWT.
    *   `BACKBONE_USERNAME`: Usuario configurado.
    *   `BACKBONE_PASSWORD`: Contraseña asociada a Backbone.
*   **Mercury Integration:**
    *   `MERCURY_GRANT_TYPE`: Tipo de concesión OAuth para Mercury.
    *   `MERCURY_SCOPE`: Alcance (scope) para Mercury.
    *   `MERCURY_CLIENT_SECRET`: Secreto del cliente de Mercury.
    *   `MERCURY_CLIENT_ID`: ID del cliente de Mercury.
    *   `MERCURY_USERNAME`: Usuario configurado.
    *   `MERCURY_PASSWORD`: Contraseña asociada a Mercury.
*   **Ressource Server JWT:**
    *   `AUTH_SERVER_URI` / `${spring.security.oauth2.resourceserver.jwt.issuer-uri}`: URI del emisor de tokens (Issuer).
    *   `AUTH_CERT_URI`: URI que apunta a los certificados JWK del servidor de recursos.
*   **OAuth Client:**
    *   `AUTH_CLIENT_ID`: ID del cliente para el perfil 'external'.
    *   `AUTH_CLIENT_SECRET`: Secreto del cliente para el perfil 'external'.

## ☁️ Configuración Cloud y Nube (Cloud & Environment)
Variables relacionadas con Spring Cloud, Vault y Redis.

*   **Spring Boot Profile:**
    *   `SPRING_BOOT_PROFILE_ACTIVE`: Perfil activo de la aplicación (`dev`, `qa`, `prod`).
*   **Config Server / Discovery:**
    *   `VAULT_ENABLED`: Indica si se debe usar HashiCorp Vault.
    *   `VAULT_TOKEN`: Token de acceso a Vault.
    *   `VAULT_SERVER_URI`: URI del servidor de Vault.
    *   `umdc.app.name`: Nombre de la aplicación usado internamente (ej: `directory-backend`).
    *   `SPRING_BOOT_CLOUD_BOOTSTRAP_ENABLED`: Habilita el contexto de bootstrap en Spring Cloud Config.
    *   `SPRING_CLOUD_CONFIG_LABEL`: Etiqueta para obtener la configuración del servidor.
    *   `CNFS_URI`: URI si se usa un almacén de configuración específico (ej: Consul/Config Server).
    *   `CNFS_PORT`: Puerto de Configuración.
*   **Redis Caching:**
    *   `REDIS_PORT`: Puerto del servidor Redis.
    *   `REDIS_PASSWORD`: Contraseña para el acceso a Redis.
    *   `REDIS_TIMEOUT`: Tiempo de espera (timeout) de Redis.
    *   `REDIS_SSL_ENABLED`: Habilita o deshabilita SSL en la conexión a Redis.
    *   `REDIS_URL`: URL completa del servidor Redis.

## ⚙️ Sistema e Infraestructura (System & Global)
Variables generales de rendimiento y logging.

*   `LOGGING_TRACE_ENABLED`: Controla si el nivel de traza (`DEBUG`) está habilitado para logging.
*   `APP_CLIENT_ROLES`: Roles del cliente en la gestión Spring Boot/Security.
*   `APP_PORT`: Puerto HTTP por defecto donde corre el servicio.

---
***Nota Importante:*** *Los placeholders `${...}` indican que estos valores deben ser inyectados al momento de la ejecución o durante el despliegue desde un entorno dinámico.*
