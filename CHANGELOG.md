# CHANGELOG - Pedidos360

## [1.6.0] - 2026-10-07 — Login real, correo Gmail y stack verificado end-to-end
### Añadido
- **`environment.docker.ts`** (nuevo): perfil de build para el contenedor frontend con **login real de Microsoft Entra ID** (`authBypass: false`), `redirectUri http://localhost:4200/` y URLs de localhost (18081/8082/8083). Se elige con la variable `NG_BUILD_CONFIG=docker` del `.env`.
- **`NG_BUILD_CONFIG` en el frontend**: `Dockerfile` con `ARG NG_BUILD_CONFIG=production` y `docker-compose.yml` con `NG_BUILD_CONFIG: ${NG_BUILD_CONFIG:-production}`. Sin `.env` el build es de producción (`environment.prod.ts` = login + API Gateway) — listo para que Tomy suba a AWS sin cambios.
- **Frontend en Docker publica `80:80` y `4200:80`**: el `4200` es la URI de redirección que Azure tiene registrada para pruebas locales y el login así funciona sin tocar nada.
- **Fix de Kafka en modo KRaft**: `KAFKA_LISTENERS` sin IP (solo puerto) porque el modo KRaft de `apache/kafka:3.9.0` rechaza `0.0.0.0` como host no enrutable; ahora `ms-monitoreo` consume el tópico y el monitoreo queda end-to-end.
- **Correo real por Gmail (SMTP) configurado**: credenciales solo en el `.env` (`.env.example` documenta `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM`); verificado enviando 4 correos reales a `diegoxmegalala@gmail.com` (pedidos registrado/aprobado/rechazado), colas en 0.
- `.env.example`, `ayuda.txt`, `GUIA-DESPLIEGUE.txt` y `README.md` actualizados con el nuevo build, los puertos 80/4200 y el estado real de lo verificado.

### Verificado (2026-10-07, stack completo de 9 contenedores)
- Kafka + monitoreo end-to-end: comprar → evento visible en `GET /monitoreo/eventos`.
- Correos reales por SMTP Gmail vía RabbitMQ → `ms-email`.
- Login real habilitado (backends devuelven 401 sin JWT; el frontend en 4200 con `authBypass: false` redirige al login de Microsoft).

## [1.5.0] - 2026-10-05 — Monitoreo con Kafka
### Añadido
- **Kafka como log de eventos de negocio (monitoreo)**, en paralelo a RabbitMQ (que sigue siendo quien entrega los correos): el tópico `pedidos360.monitoreo` recibe los mismos 4 eventos que la cola (`PEDIDO_REGISTRADO`, `PAGO_APROBADO`, `PAGO_RECHAZADO`, `CAMBIO_ESTADO`) con clave = `pedidoId` para que todos los eventos de un pedido se escriban en la misma partición y se lean en orden.
- **Productor en `ms-carrito`**: dependencia `spring-kafka`, variable `KAFKA_BOOTSTRAP_SERVERS` (por defecto `localhost:9092`) y clase `messaging/MonitorPublisher.java`, llamada desde `CarritoController` en checkout, pago aprobado, pago rechazado y cambio de estado. Si Kafka no está disponible solo se registra el error: la compra nunca falla.
- **Nuevo microservicio `ms-monitoreo`** (Spring Boot, puerto 8086, `Dockerfile` propio): consume el tópico con `@KafkaListener` (grupo de consumidor `ms-monitoreo`, JSON ilegible no detiene al consumidor), expone los últimos 500 eventos con su `topico`/`particion`/`offset` en `EventoMonitor`, mantiene contadores acumulados por tipo (`EventoStore`) y publica `GET /monitoreo/eventos?tipo=...` y `GET /monitoreo/estadisticas`; CORS y bypass igual que el resto de servicios.
- **`docker-compose.yml`**: servicio `kafka` (`apache/kafka:3.9.0` en modo KRaft, sin ZooKeeper, auto-creación de tópico, expuesto solo en `127.0.0.1:9094`), servicio `ms-monitoreo` (`depends_on: kafka`) y `KAFKA_BOOTSTRAP_SERVERS: kafka:9092` para `ms-carrito`. Total del stack: 9 contenedores.
- **6 pruebas nuevas en `ms-monitoreo`** (`EventoStoreTest` 4, `EventoConsumerTest` 2); `ms-carrito` sigue en 21. Total del proyecto: 54 pruebas en verde.
- **`KAFKA-MONITOREO.txt`**: explicación de qué es Kafka y cómo funciona acá, tabla RabbitMQ vs Kafka, diagrama de flujo, API del monitoreo con ejemplos JSON, receta de verificación paso a paso, qué quedó verificado/pendiente y guion de presentación de 60 s.
- `README.md`, `ayuda.txt` y `GUIA-DESPLIEGUE.txt` actualizados con `ms-monitoreo`, el puerto 8086, el servicio `kafka` y la sección de monitoreo.

### Limitaciones conocidas
- El flujo end-to-end con broker real (comprar y ver el evento en `GET /monitoreo/eventos`) está **pendiente de ejecutar**; lo verificado hasta ahora es la compilación, las 6 pruebas unitarias de `ms-monitoreo`, las 21 de `ms-carrito` y la validez del `docker-compose.yml`. La receta exacta queda en `KAFKA-MONITOREO.txt` punto 7.
- `ms-monitoreo` guarda los eventos en memoria (ventana de 500): al reiniciarse se pierde el histórico local, pero el log completo permanece en Kafka.

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