# CHANGELOG - Pedidos360

## [1.4.0] - 2026-10-01 — Segunda evaluación
### Añadido
- **Métodos de pago simulados** en `ms-carrito`: `EstadoPago` (`PENDIENTE`/`APROBADO`/`RECHAZADO`), `MetodoPago` (`TARJETA_CREDITO`/`TARJETA_DEBITO`/`TRANSFERENCIA`) y flujo de checkout en dos pasos sin duplicar pedidos: `POST /carrito/checkout` crea el pedido en `PENDIENTE_PAGO` y vacía el carrito; `POST /pedidos/{id}/pago` aprueba (`CONFIRMADO`+`APROBADO`) o rechaza (`CANCELADO`+`RECHAZADO`, restaurando el carrito). Un pago repetido devuelve 400.
- **Estados de pedido ampliados**: `PENDIENTE_PAGO`, `CONFIRMADO` (pagado), `EN_PREPARACION`, `ENVIADO`, `ENTREGADO`, `CANCELADO`; `PUT /pedidos/{id}/estado` para avanzar estados con validación de transiciones.
- **Gestión completa de cantidades del carrito**: `PUT /carrito/items/{id}` para aumentar/disminuir, validación de cantidades y de stock, y propiedad por usuario (403 si un usuario toca el carrito de otro).
- **Catálogo con filtros e imágenes**: `imagenUrl` en `Producto`, `GET /productos/categorias`, filtros combinados `?nombre=&categoria=` en `GET /productos` y datos de ejemplo con fotografías reales.
- **Tres eventos de correo** publicados por `ms-carrito`: `PEDIDO_REGISTRADO`, `PAGO_APROBADO`, `PAGO_RECHAZADO` (una sola vez por evento), con plantillas HTML en `PlantillaEmails`.
- **Reintentos y DLQ**: cola `pedidos360.emails` con `x-dead-letter-*` a `pedidos360.emails.dlq` declarada en ambos servicios, y `spring.rabbitmq.listener.simple` con 3 intentos en `ms-email`.
- **Nuevo microservicio `ms-pagos`** (Spring Boot, puerto 8085): microservicio separado que decide y registra el pago simulado (`POST /pagos`, `GET /pagos/pedido/{id}`), con entidad `Pago` (a lo sumo uno por pedido), JWT de Azure validado (ms-carrito reenvía el token), CORS y 8 pruebas unitarias. `ms-carrito` lo consume con `PagoClient` (`PAGOS_SERVICE_URL`) y solo aplica el resultado sobre el pedido; si `ms-pagos` no responde, el pago no se aprueba.
- **Frontend rediseñado como tienda comercial**: página de inicio (banner, categorías, destacados, footer), catálogo con chips de categoría y búsqueda, carrito con stepper de cantidades y resumen, **checkout con selección de método de pago** y resultados de compra, historial de pedidos filtrable y expandible, navbar con buscador/campana/contador de carrito y notificaciones visuales (toasts + campana).
- `.env.example` con todas las variables (sin credenciales) y volúmenes `carrito-data` y `pagos-data` en `docker-compose.yml` para que pedidos y pagos sobrevivan a reinicios.

### Corregido
- El evento de correo del checkout pasó de `PEDIDO_CONFIRMADO` a `PEDIDO_REGISTRADO`: con el pago simulado, el checkout ya no "confirma" la compra, solo la registra.
- `PRODUCTOS_SERVICE_URL` en `docker-compose.yml` (apuntaba a `localhost`, dentro del contenedor debe ser `http://ms-productos:8081/productos`).
- CORS de `ms-identidad` alineado con el resto de servicios (incluye el origen desplegado en `https://44.200.146.153`).
- `sourceEncoding` UTF-8 en los cuatro `pom.xml` (evita errores de compilación por tildes).
- Import de `filter` faltante en `navbar.component.ts` y ruta de importación de `toast.service` en `toast-container.component.ts` (el build de Angular fallaba).

### Cambiado
- `environment.ts` (local) queda con `authBypass: false`: el login con Microsoft Entra ID es el comportamiento por defecto también en local; `true` queda documentado como alternativa solo para pruebas sin Azure.
- `DatosDeEjemplo` (ms-productos) ya no depende de `AUTH_BYPASS`: siembra los 8 productos cuando la base arranca vacía (variable `CATALOGO_SEED`, por defecto `true`), para que el catálogo no salga en blanco en el primer despliegue con autenticación real.
- `GUIA-DESPLIEGUE.txt` ampliada con la sección **0.1 Dónde cambiar el tenant / client ID** (tabla archivo → línea exacta para un registro de Azure nuevo) y con el checklist de verificación actualizado al flujo real de checkout en dos pasos.

### Limitaciones conocidas
- El pago es **simulado**: no hay conexión con bancos ni se piden datos reales de tarjeta (los del formulario solo existen en el navegador).
- Con Docker Desktop + WSL2 instalados en el entorno local, `docker compose up -d --build` quedó verificado end-to-end (7 contenedores, compra aprobada/rechazada, RabbitMQ publica → consume → 3 reintentos → `pedidos360.emails.dlq`). Sigue sin verificarse el envío real por SMTP (falta una app password) y el login real con Microsoft Entra ID.

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