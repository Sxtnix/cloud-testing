# Pedidos360 — E-Commerce Cloud Native

Repositorio del proyecto desarrollado para la asignatura **Desarrollo Cloud Native I (DSY1107)** — Evaluación Parcial N°1.

Plataforma de e-commerce con arquitectura de microservicios, autenticación centralizada mediante **Azure Entra ID (MSAL)** y frontend en **Angular**.

## Tecnologías

* Angular 17 (standalone components) + MSAL Angular
* Java 17 + Spring Boot 3.2 (microservicios)
* Spring Data JPA + MySQL
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
    emitido por Azure AD)                               │ PEDIDO_CONFIRMADO
                                                        ▼
                                               ┌─────────────────┐
                                               │    RabbitMQ      │
                                               │ pedidos360.emails│
                                               └────────┬────────┘
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

## Estructura del repositorio

```
├── ms-productos/          # Microservicio de catálogo de productos
├── ms-identidad/          # Microservicio de perfil de usuario (vinculado al login de Azure AD)
├── ms-carrito/            # Microservicio de carrito de compras y pedidos
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
| GET | `/productos` | Lista todos los productos (filtros opcionales `?categoria=` `?nombre=`) |
| GET | `/productos/{id}` | Obtiene un producto |
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
| POST | `/carrito/items` | Agrega un producto al carrito |
| DELETE | `/carrito/items/{id}` | Elimina un ítem del carrito |
| POST | `/carrito/checkout` | Confirma la compra, genera un Pedido y publica `PEDIDO_CONFIRMADO` en RabbitMQ |
| GET | `/pedidos` | Historial de pedidos del usuario |

**ms-email** (`/emails`)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/emails/enviar` | Envío manual de un correo (pruebas/operación). El flujo normal consume la cola `pedidos360.emails` de forma asíncrona |

### Flujo de correo asíncrono (RabbitMQ)
1. El usuario confirma su compra (`POST /carrito/checkout`).
2. `ms-carrito` publica un mensaje JSON `{destino, asunto, cuerpo, tipo}` en la cola durable `pedidos360.emails`.
3. `ms-email` consume el mensaje y envía el correo por SMTP (`MAIL_*`).
4. Si RabbitMQ o el SMTP no están disponibles, la compra **no falla**: el error queda registrado en los logs.

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
apiProductosUrl: 'http://localhost:8081/productos', // o la URL del API Gateway
apiIdentidadUrl: 'http://localhost:8082/usuarios',
apiCarritoUrl: 'http://localhost:8083'
```

### 3. Configurar cada microservicio backend
Cada `ms-x/src/main/resources/application.yml` usa variables de entorno con valores por defecto. Al desplegar en EC2, definir:
- `AZURE_ISSUER_URI` → `https://login.microsoftonline.com/<TENANT_ID>/v2.0`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` → apuntando a la base de datos cloud (RDS u otro motor).
- `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` → broker de mensajes (en docker-compose ya apuntan al servicio `rabbitmq`).
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` → (**solo ms-email**) credenciales SMTP, p. ej. una app password de Gmail. **Nunca** commitearlas: usar un archivo `.env` (docker-compose las lee de ahí) o secretos del servidor.

### 4. AWS
- Desplegar cada microservicio (`ms-productos`, `ms-identidad`, `ms-carrito`, `ms-email`) en instancias EC2 (cada uno trae su `Dockerfile`, o se puede correr el `.jar` directo con `mvn clean package` y `java -jar`).
- Desplegar un broker RabbitMQ (p. ej. un contenedor `rabbitmq:3-management` o un servicio gestionado) y apuntar `RABBITMQ_HOST` hacia él.
- Configurar **API Gateway** para enrutar hacia cada EC2 (ej. `/productos/*` → ms-productos, `/usuarios/*` → ms-identidad, `/carrito/*` y `/pedidos/*` → ms-carrito). `ms-email` no necesita ruta pública: solo consume la cola y habla con el SMTP.
- Desplegar el frontend Angular compilado (`npm run build`, carpeta `dist/`) en S3+CloudFront, Amplify, o servirlo con el `Dockerfile`/`nginx.conf` incluido.

### 5. Ejecución local para probar antes de desplegar
```bash
# Broker de mensajes (una sola vez)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=pedidos360 -e RABBITMQ_DEFAULT_PASS=pedidos360 \
  rabbitmq:3.13-management-alpine

# Cada microservicio
cd ms-productos && mvn spring-boot:run   # puerto 8081
cd ms-identidad && mvn spring-boot:run   # puerto 8082
cd ms-carrito && mvn spring-boot:run     # puerto 8083
cd ms-email && mvn spring-boot:run       # puerto 8084 (definir MAIL_USERNAME / MAIL_PASSWORD)

# Frontend
cd pedidos360-frontend && npm install && npm start   # puerto 4200

# O todo junto con Docker
docker compose up --build
```

## Integrantes

* Thomas Alvarez
* Diego Diaz
