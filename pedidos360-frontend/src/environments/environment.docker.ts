export const environment = {
  production: true,

  // DEMO LOCAL con LOGIN REAL (docker compose): exige login con Microsoft Entra ID.
  // Usada solo por `docker compose up` (build con --configuration=docker).
  // NO afecta a environment.ts (local/ng serve) ni environment.prod.ts (despliegue real).
  authBypass: false,

  // MSAL se instancia (auth-config.ts / APP_INITIALIZER) con los IDs reales.
  azureClientId: 'bcb83cb2-3d6f-4715-b3e7-204237e6f53d',
  azureTenantId: '9150f81f-b0fc-436d-998d-dcb5cf883d2e',
  azureApiScope: 'api://bcb83cb2-3d6f-4715-b3e7-204237e6f53d/write-read',

  redirectUri: 'http://localhost:4200/',

  // El navegador (host) llega a los contenedores via puertos mapeados en docker-compose.
  // 18081 -> ms-productos (8081), 8082 -> ms-identidad, 8083 -> ms-carrito.
  apiProductosUrl: 'http://localhost:18081/productos',
  apiIdentidadUrl: 'http://localhost:8082/usuarios',
  apiCarritoUrl: 'http://localhost:8083'
};
