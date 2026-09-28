# CHANGELOG - Pedidos360

## [1.3.0] - 2026-09-28
### Añadido
- Nuevo microservicio **`ms-email`** (Spring Boot, puerto 8084): consume la cola durable `pedidos360.emails` en RabbitMQ y envía los correos por SMTP (`spring-boot-starter-mail`), con endpoint manual `POST /emails/enviar`, validación de JWT de Azure AD y pruebas unitarias (`EmailServiceTest`, `EmailListenerTest`).
- Integración de **RabbitMQ** en `ms-carrito`: al confirmar el checkout se publica el evento `PEDIDO_CONFIRMADO` (`{destino, asunto, cuerpo, tipo}`) en JSON. Si el broker no está disponible la compra no falla, solo se registra el error.
- Servicio `rabbitmq:3.13-management-alpine` en `docker-compose.yml` (AMQP 5672 + consola 15672) y variables `RABBITMQ_HOST/USERNAME/PASSWORD` para `ms-carrito` y `ms-email`.
- Credenciales SMTP por variables de entorno (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`), eliminando secretos del código.

## [1.2.0] - 2026-09-07
### Añadido
- Migración del frontend a **Angular**, implementando componentes modulares, enrutamiento, servicios tipados (`producto.service.ts`, `carrito.service.ts`, `usuario.service.ts`) y configuración moderna con MSAL para autenticación.
- Creación completa del microservicio **`ms-carrito`** (Spring Boot), incluyendo gestión de carritos, ítems, pedidos y modelos de persistencia JPA.
- Creación del microservicio **`ms-identidad`** (Spring Boot) para el manejo de usuarios y roles del sistema.
- Implementación completa del microservicio **`ms-productos`**, que hasta la versión anterior solo contaba con clases vacías: entidad, repositorio, servicio y controlador con endpoints CRUD funcionales.
- Implementación de pruebas unitarias para la lógica de servicios (`ProductoServiceTest`, `UsuarioServiceTest`, `CarritoServiceTest`).
- Creación de contenedores individuales mediante **Dockerfiles** para cada microservicio y el frontend, junto con la configuración de Nginx.
### Corregido
- Ruta del `application.yml` de `ms-productos`, que estaba en `src/resources/` y no era reconocida por Maven; se movió a `src/main/resources/`.
### Pendiente
- Registro de la aplicación en Azure Entra ID (App Registration, client ID, tenant ID, scope).
- Configuración de AWS API Gateway como entrypoint hacia los microservicios en EC2.
- Despliegue de los microservicios en EC2 y del frontend.
- Conexión a la base de datos cloud definitiva.

## [1.1.0] - 2026-08-24
### Añadido
- Microservicio de Productos (Java / Spring Boot): estructura inicial de clases y paquetes.
- Configuración de dependencias en pom.xml (Spring Boot 3.x).
- Esqueleto de configuración de seguridad OAuth2 con Customizer.withDefaults() actualizada a Spring Security 6.1+.
### Corregido
- Corrección de la estructura de paquetes Java (com.pedidos360.productos) y resolución de dependencias de Maven.

## [1.0.0] - 2026-08-24
### Añadido
- Estructura inicial del proyecto: frontend en React (scaffold de páginas Login, Catálogo y Carrito) y esqueleto de microservicios de Productos y Carrito en Spring Boot.
- Definición de la arquitectura objetivo: autenticación con Azure AD (MSAL), backend en Spring Boot sobre EC2, expuesto mediante AWS API Gateway.