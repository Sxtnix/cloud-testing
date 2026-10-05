# Pedidos360 — E-Commerce Cloud Native

Repositorio del proyecto desarrollado para la asignatura **Desarrollo Cloud Native I (DSY1107)** — Evaluación Parcial N°1 y **Segunda Evaluación**.

Plataforma de e-commerce con arquitectura de microservicios, autenticación centralizada mediante **Azure Entra ID (MSAL)**, frontend en **Angular**, **pagos simulados** con tres métodos de pago y notificaciones por correo vía **RabbitMQ**.

## Tecnologías

* Angular 17 (standalone components) + MSAL Angular
* Java 17 + Spring Boot 3.2 (microservicios)
* Spring Data JPA + H2 en memoria (por defecto; configurable a MySQL/PostgreSQL con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
* RabbitMQ (mensajería: envío asíncrono de correos)
* Docker
* AWS (EC2 + API Gateway) / Microsoft Azure (Entra ID)
* Git / GitHub

## Arquitectura

```
                     ┌───────────────────────┐
                     │   Angular Frontend     │
                     │  (Azure AD / MSAL)     │
                     └───────────┬───────────┘
                                 │ JWT (Bearer)
                                 ▼
                     ┌───────────────────────┐
                     │   AWS API Gateway      │
                     └───────────┬───────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        ▼                        ▼                         ▼
┌───────────────┐      ┌───────────────┐        ┌───────────────┐
│ ms-productos   │      │ ms-identidad  │        │  ms-carrito    │
│ (puerto 8081)  │      │ (puerto 8082) │        │  (puerto 8083) │
└───────────────┘      └───────────────┘        └───────┬───────┘
   (EC2 + Spring Boot, cada uno valida el JWT           │ checkout publica
    emitido por Azure AD)                               │ eventos de correo
                                                       │
                                        ┌──────────────┴──────────────┐
                                        │ llama a POST /pagos         │
                                        ▼                             ▼
                               ┌───────────────┐            ┌─────────────────┐
                               │   ms-pagos     │            │    RabbitMQ      │
                               │ (puerto 8085)  │            │ pedidos360.emails│
                               └───────────────┘            └────────┬────────┘
                                                                     │ consume
                                                                     ▼
                                                            ┌───────────────┐
                                                            │   ms-email     │
                                                            │  (puerto 8084) │
                                                            └───────┬───────┘
                                                                    │ SMTP
                                                                    ▼
                                                               Bandeja del
                                                               cliente
```

**Eventos publicados en la cola `pedidos360.emails`** (uno por hecho de negocio, sin correos duplicados):

| Tipo | Cuándo se publica |
|---|---|
| `PEDIDO_REGISTRADO` | El checkout crea el pedido (queda pendiente de pago) |
| `PAGO_APROBADO` | El pago simulado fue aprobado |
| `PAGO_RECHAZADO` | El pago simulado fue rechazado (pedido cancelado) |
| `CAMBIO_ESTADO` | Cambio de estado logístico (en preparación / enviado / entregado) |

Si el broker no está disponible, el error solo se registra en los logs: la compra nunca falla por RabbitMQ. Los mensajes fallidos van a la cola de respaldo `pedidos360.emails.dlq` tras 3 intentos.

## Estructura del repositorio

```
├── ms-productos/          # Microservicio de catálogo de productos
├── ms-identidad/          # Microservicio de perfil de usuario (vinculado al login de Azure AD)
├── ms-carrito/            # Microservicio de carrito de compras y pedidos
├── ms-pagos/              # Microservicio de pagos (decide y registra el resultado del pago)
├── ms-email/              # Microservicio de correos (consume RabbitMQ y envía por SMTP)
└── pedidos360-frontend/   # SPA en Angular con MSAL
```

Cada microservicio de backend sigue la misma estructura interna:
```
ms-x/
├── pom.xml
├── Dockerfile
├── .gitignore
└── src/
    ├── main/java/com/pedidos360/x/
    │   ├── XApplication.java      # Clase principal Spring Boot
    │   ├── config/SecurityConfig.java   # Valida el JWT de Azure AD
    │   ├── model/                 # Entidades JPA
    │   ├── repository/            # Repositorios JPA
    │   ├── service/                # Lógica de negocio
    │   └── controller/            # Endpoints REST
    ├── main/resources/application.yml
    └── test/java/...              # Pruebas unitarias básicas
```

## Endpoints principales

**ms-productos** (`/productos`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/productos` | Catálogo. Filtros opcionales y combinables `?nombre=` `?categoria=` (búsqueda sin distinguir mayúsculas ni tildes) |
| GET | `/productos/categorias` | Lista de categorías existentes (para los filtros del frontend) |
| GET | `/productos/{id}` | Detalle de un producto |
| POST | `/productos` | Crea un producto |
| PUT | `/productos/{id}` | Actualiza un producto |
| DELETE | `/productos/{id}` | Elimina un producto |

**ms-identidad** (`/usuarios`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/usuarios/me` | Perfil del usuario autenticado (se crea automáticamente la primera vez) |
| GET | `/usuarios` | Lista todos los usuarios |
| PUT | `/usuarios/{id}/rol` | Cambia el rol de un usuario |

**ms-carrito** (`/carrito`, `/pedidos`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/carrito` | Carrito activo del usuario autenticado |
| POST | `/carrito/items` | Agrega un producto (precio, nombre e imagen se toman de `ms-productos`) |
| PUT | `/carrito/items/{id}` | Aumenta o disminuye la cantidad (valida stock y cantidad > 0) |
| DELETE | `/carrito/items/{id}` | Elimina un ítem del carrito |
| POST | `/carrito/checkout` | Crea el pedido en `PENDIENTE_PAGO`, vacía el carrito y publica `PEDIDO_REGISTRADO` |
| POST | `/pedidos/{id}/pago` | **Pago simulado**: delega la decisión en `ms-pagos` y aplica el resultado — aprueba (`CONFIRMADO`/`APROBADO`) o rechaza (`CANCELADO`/`RECHAZADO`, restaura el carrito). Body `{simularRechazo: boolean}` |
| GET | `/pedidos` | Historial de pedidos del usuario (más reciente primero) |
| GET | `/pedidos/{id}` | Detalle de un pedido (solo el propietario, 403 si no lo es) |
| PUT | `/pedidos/{id}/estado` | Avanza el seguimiento logístico: `CONFIRMADO → EN_PREPARACION → ENVIADO → ENTREGADO` (400 si la transición no está permitida). Body `{estado: "..."}` |

**ms-pagos** (`/pagos`) — microservicio separado que decide y registra el pago; lo llama `ms-carrito` (no se expone en el API Gateway)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/pagos` | Registra (simula) el pago de un pedido: aprueba o rechaza, con un solo pago por pedido. Body `{pedidoId, usuarioId, monto, metodoPago, simularRechazo}` |
| GET | `/pagos/pedido/{id}` | Pago registrado para un pedido (auditoría/soporte). 404 si aún no existe |

**ms-email** (`/emails`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/emails/enviar` | Envío manual de un correo (pruebas/operación). El flujo normal consume la cola `pedidos360.emails` de forma asíncrona |

### Flujo de correo asíncrono (RabbitMQ)
1. El usuario confirma su compra (`POST /carrito/checkout`) o se produce un evento de pago/cambio de estado.
2. `ms-carrito` publica un mensaje JSON `{destino, asunto, cuerpo, tipo}` en la cola durable `pedidos360.emails` (con plantilla HTML).
3. `ms-email` consume el mensaje y envía el correo por SMTP (`MAIL_*`).
4. Si RabbitMQ o el SMTP no están disponibles, la compra **no falla**: el error queda registrado en los logs.
5. Tras 3 intentos fallidos, el mensaje pasa a la cola de respaldo `pedidos360.emails.dlq` (`x-dead-letter-*`, declarada igual en ambos servicios).

**Cómo probar los correos sin credenciales reales:**
- Sin `MAIL_USERNAME`/`MAIL_PASSWORD`: el envío falla y el mensaje queda reintentando → se ven los logs de `ms-email` y la DLQ en la consola de RabbitMQ (`http://localhost:15672`). La compra sigue funcionando.
- Envío manual rápido (no depende de RabbitMQ): `POST /emails/enviar` con `{destino, asunto, cuerpo}` y revisar el log de `ms-email`.
- Para ver los correos de verdad: crear una *app password* de Gmail en `.env` (`MAIL_USERNAME`, `MAIL_PASSWORD`) y reiniciar solo `ms-email`.

Todos los endpoints (salvo que se indique lo contrario) requieren un JWT válido emitido por Azure AD en el header `Authorization: Bearer <token>`.

---

## ⚙️ Lo que falta configurar en Azure/AWS antes de desplegar

El código está completo y probado a nivel de compilación/lógica. Lo único que falta es **conectar con los servicios reales de Azure y AWS**, que son credenciales/infraestructura que no se pueden generar de antemano:

### 1. Registrar la app en Azure Entra ID
Sigue los mismos pasos del `tutorial-estudiantes.html` que mandó el profe:
1. Azure Portal → Microsoft Entra ID → Registros de aplicaciones → Nuevo registro (ej. `pedidos360-api`).
2. Copiar el **Application (client) ID** y el **Directory (tenant) ID**.
3. En *Exponer una API* → Agregar un ámbito → nombre `write-read` (sin barras).
4. En *Aplicaciones cliente autorizadas* → agregar el mismo Client ID y marcar `write-read`.
5. En *Certificados y secretos* → generar un secreto si hace falta (para llamadas server-to-server; no es necesario para el flujo SPA con MSAL).
6. En *Autenticación* → Agregar plataforma → **Aplicación de página única (SPA)** → poner la URI de redirección (`http://localhost:4200/` para pruebas locales, y la URL pública una vez desplegado en AWS).

### 2. Configurar el frontend (`pedidos360-frontend`)
Editar `src/environments/environment.ts` (y `environment.prod.ts` para producción) y reemplazar:
```ts
azureClientId: 'TU_CLIENT_ID_AQUI',
azureTenantId: 'TU_TENANT_ID_AQUI',
azureApiScope: 'api://TU_CLIENT_ID_AQUI/write-read',
redirectUri: 'http://localhost:4200/',           // o la URL pública en prod
apiProductosUrl: 'http://localhost:18081/productos', // local: 2da instancia de ms-productos (o 8081 con docker compose / API Gateway en EC2)
apiIdentidadUrl: 'http://localhost:8082/usuarios',
apiCarritoUrl: 'http://localhost:8083'
```

### 3. Configurar cada microservicio backend
Cada `ms-x/src/main/resources/application.yml` usa variables de entorno con valores por defecto. Al desplegar en EC2, definir:
- `AZURE_ISSUER_URI` → `https://login.microsoftonline.com/<TENANT_ID>/v2.0`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` → apuntando a la base de datos cloud (RDS u otro motor).
- `PRODUCTOS_SERVICE_URL`, `PAGOS_SERVICE_URL` → cómo se llaman entre sí los microservicios (en docker-compose ya apuntan a los servicios `ms-productos` y `ms-pagos`).
- `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` → broker de mensajes (en docker-compose ya apuntan al servicio `rabbitmq`).
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` → (**solo ms-email**) credenciales SMTP, p. ej. una app password de Gmail. **Nunca** commitearlas: usar un archivo `.env` (docker-compose las lee de ahí) o secretos del servidor.

### 4. AWS
- Desplegar cada microservicio (`ms-productos`, `ms-identidad`, `ms-carrito`, `ms-pagos`, `ms-email`) en instancias EC2 (cada uno trae su `Dockerfile`, o se puede correr el `.jar` directo con `mvn clean package` y `java -jar`).
- Desplegar un broker RabbitMQ (p. ej. un contenedor `rabbitmq:3-management` o un servicio gestionado) y apuntar `RABBITMQ_HOST` hacia él.
- Configurar **API Gateway** para enrutar hacia cada EC2 (ej. `/productos/*` → ms-productos, `/usuarios/*` → ms-identidad, `/carrito/*` y `/pedidos/*` → ms-carrito). `ms-email` y `ms-pagos` **no necesitan ruta pública**: el primero solo consume la cola y habla con el SMTP, y el segundo solo lo llama `ms-carrito` por la red interna.
- Desplegar el frontend Angular compilado (`npm run build`, carpeta `dist/`) en S3+CloudFront, Amplify, o servirlo con el `Dockerfile`/`nginx.conf` incluido.

### 5. Ejecución local para probar antes de desplegar
```bash
# Broker de mensajes (una sola vez)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=pedidos360 -e RABBITMQ_DEFAULT_PASS=pedidos360 \
  rabbitmq:3.13-management-alpine

# Cada microservicio (default AUTH_BYPASS=false => login real de Microsoft Entra ID)
cd ms-productos && mvn spring-boot:run   # puerto 8081
cd ms-identidad && mvn spring-boot:run   # puerto 8082
cd ms-carrito   && mvn spring-boot:run   # puerto 8083
cd ms-email     && mvn spring-boot:run   # puerto 8084 (MAIL_* opcionales)
cd ms-pagos     && mvn spring-boot:run   # puerto 8085 (lo llama ms-carrito al pagar)

# Segunda instancia de ms-productos en 18081: es la URL que usa el
# frontend en local (pedidos360-frontend/src/environments/environment.ts)
cd ms-productos && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18081

# Frontend
cd pedidos360-frontend && npm install && npm start   # puerto 4200

# O todo junto con Docker
docker compose up --build
```
> En Windows (PowerShell) reemplaza `VAR=valor cmd` por `$env:VAR='valor'; cmd`.
>
> **Modo de autenticación.** El proyecto ya viene con `authBypass: false` (frontend) y
> `AUTH_BYPASS=false` (backends), es decir, **login real con Microsoft Entra ID** en local
> y en AWS: hay que iniciar sesión en `http://localhost:4200` antes de abrir el catálogo.
> Para probar **sin Azure**, levanten los backends con `AUTH_BYPASS=true` y pongan
> `authBypass: true` en `pedidos360-frontend/src/environments/environment.ts` (solo ese
> archivo local; **nunca** en `environment.prod.ts`).
>
> **Catálogo:** si la base arranca vacía se siembra sola con 8 productos de ejemplo
> (`CATALOGO_SEED=false` para apuntar a una BD con productos reales).
> Para desplegar en AWS con otro tenant/registro de Azure, ver `GUIA-DESPLIEGUE.txt`
> (sección 0.1 = archivo por archivo dónde cambiar el client ID y el tenant ID).

## Compra de prueba (demostración del flujo completo)

1. `docker compose up --build` (o los 6 procesos de arriba) y entrar a `http://localhost:4200`.
   Con `authBypass: false` hay que hacer **Iniciar sesión con Microsoft** (cuenta del tenant)
   antes de abrir el catálogo; si no, el interceptor de MSAL retiene las llamadas a la API.
2. **Catálogo** → buscar/filtrar por categoría → *Agregar* 2 productos (aparece un toast y el contador de la navbar).
3. **Carrito** → subir/bajar cantidades con el stepper → verificar subtotal y total → *Continuar al checkout*.
4. **Checkout** → elegir *Tarjeta de crédito / débito / transferencia* → completar los datos de prueba
   (`4111 1111 1111 1111`, cualquier titular, `12/30`, `123`; **no se envían al backend**) → *Confirmar y pagar*.
5. Pantalla de resultado: **Pago aprobado** → el pedido pasa a `CONFIRMADO`/`APROBADO`.
6. **Mis pedidos** → expandir el pedido: fecha, método de pago, estado del pedido y del pago, detalle de productos.
7. Para ver el camino de error: activar *Modo demo: simular pago rechazado* en el checkout → el pedido queda
   `CANCELADO`/`RECHAZADO`, se muestra el aviso y **el carrito se restaura** con los mismos productos.
8. Variante por API (útil para la defensa): avanzar el estado logístico del pedido aprobado
   ```bash
   curl -X PUT http://localhost:8083/pedidos/1/estado -H "Content-Type: application/json" -d '{"estado":"EN_PREPARACION"}'
   ```
   (`CONFIRMADO → EN_PREPARACION → ENVIADO → ENTREGADO`; una transición inválida devuelve `400`).

## Pruebas ejecutadas

| Suite | Comando | Resultado |
|---|---|---|
| `ms-productos` | `mvn test` | **7 tests, 0 fallos** |
| `ms-identidad` | `mvn test` | **2 tests, 0 fallos** |
| `ms-carrito` | `mvn test` | **21 tests, 0 fallos** |
| `ms-pagos` | `mvn test` | **8 tests, 0 fallos** |
| `ms-email` | `mvn test` | **10 tests, 0 fallos** |
| Frontend | `npx ng build` | **Build exitoso** (0 errores) |

Verificación manual end-to-end sobre los servicios levantados (2026-10-02): catálogo con imágenes y filtros
sin tildes, precios tomados de `ms-productos` (se ignora el precio del navegador), acumulación de ítems,
checkout `PENDIENTE_PAGO`, carrito vacío tras el checkout, **pago delegado en `ms-pagos`** (aprobado,
rechazado con carrito restaurado y registro consultable en `GET /pagos/pedido/{id}`), pago doble → `400`,
transiciones de estado y CORS con el origen desplegado.

**Verificación con `docker compose up -d --build`** (2026-10-02, Docker Desktop + WSL2): los 7 contenedores
(`rabbitmq`, los 5 microservicios y el frontend en nginx) levantan; el catálogo siembra 8 productos; la
compra corre completa sobre el stack aprobado (`CONFIRMADO` / `APROBADO` con registro en
`GET /pagos/pedido/{id}`), el pago doble devuelve `400` y el pago rechazado deja `CANCELADO` / `RECHAZADO`
con el carrito restaurado. RabbitMQ funciona end-to-end: `ms-carrito` publica y `ms-email` consume
(4 publicados / 4 entregados) y, al no haber credenciales SMTP, agota los 3 reintentos y los mensajes caen
en `pedidos360.emails.dlq`. El `ng serve` local (4200) quedó apuntando a estos mismos contenedores.
También se verificó el modo degradado: con el broker caído los eventos se registran como error en el log y
**la compra se completa igual**.

**No se pudo verificar**: el envío real de correos por SMTP (falta una app password real en `.env`) y el
login real con Microsoft Entra ID (requiere cuenta del tenant registrado).

## Limitaciones conocidas

- El pago es **simulado**: lo decide y registra `ms-pagos` (sin integración con pasarelas bancarias) y no se
  piden ni se almacenan datos reales de tarjeta (los campos del formulario solo viven en el navegador y no se
  envían al backend).
- Las notificaciones visuales (toasts + campana) son **temporales**: viven mientras la pestaña está abierta.
  No existe un sistema persistente de notificaciones internas.
- La base de datos por defecto es H2. En `docker-compose.yml` se usan H2 en disco con los volúmenes
  `carrito-data` y `pagos-data` para que pedidos y pagos sobrevivan a reinicios; en producción se recomienda
  MySQL/PostgreSQL.

## Integrantes

* Thomas Alvarez
* Diego Diaz
