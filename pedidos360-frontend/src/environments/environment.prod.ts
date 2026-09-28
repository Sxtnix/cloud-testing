export const environment = {
  production: true,

  // false = login con Microsoft Entra ID (producción). Ver environment.ts (local).
  authBypass: false,

  // === Datos del registro de la app en Azure Entra ID ===
  azureClientId: 'bcb83cb2-3d6f-4715-b3e7-204237e6f53d',
  azureTenantId: '9150f81f-b0fc-436d-998d-dcb5cf883d2e',
  azureApiScope: 'api://bcb83cb2-3d6f-4715-b3e7-204237e6f53d/write-read',

  // URL pública donde quede desplegado el frontend. OJO: Azure exige HTTPS
  redirectUri: 'https://44.200.146.153/',

  // URL(s) del AWS API Gateway que enruta hacia cada microservicio en EC2
  apiProductosUrl: 'https://st3wqbtin7.execute-api.us-east-1.amazonaws.com/desarrollo/productos',
  apiIdentidadUrl: 'https://st3wqbtin7.execute-api.us-east-1.amazonaws.com/desarrollo/usuarios',
  apiCarritoUrl: 'https://st3wqbtin7.execute-api.us-east-1.amazonaws.com/desarrollo/carrito'
};