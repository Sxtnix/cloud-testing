export const environment = {
  production: false,

  // Prueba local con el login REAL de Microsoft Entra ID.
  // ACTUAL: false => hay que iniciar sesion (cuenta del tenant 9150f81f-...) antes
  // de abrir catalogo/carrito/checkout/pedidos, si no el MsalInterceptor bloquea
  // las llamadas y "no cargan los productos".
  // Solo poner true para pruebas SIN Azure (los microservicios deberian correr
  // entonces con AUTH_BYPASS=true). NUNCA usar true en environment.prod.ts.
  authBypass: false,

  // === Datos del registro de la app en Azure Entra ID ===
  // Azure Portal > Microsoft Entra ID > Registros de aplicaciones > tu app
  azureClientId: 'bcb83cb2-3d6f-4715-b3e7-204237e6f53d',
  azureTenantId: '9150f81f-b0fc-436d-998d-dcb5cf883d2e',

  // Scope expuesto en "Exponer una API" (Azure Portal), formato: api://<CLIENT_ID>/<nombre-scope>
  azureApiScope: 'api://bcb83cb2-3d6f-4715-b3e7-204237e6f53d/write-read',

  // URL de redirección configurada en Azure como "Aplicación de página única (SPA)"
  redirectUri: 'http://localhost:4200/',

  // URLs base de los 3 microservicios (en local).
  // En esta máquina el puerto 8081 está ocupado por un proceso del sistema,
  // por eso ms-productos se levanta en el 18081. En EC2 se usa el 8081 normal.
  apiProductosUrl: 'http://localhost:18081/productos',
  apiIdentidadUrl: 'http://localhost:8082/usuarios',
  apiCarritoUrl: 'http://localhost:8083'
};
