import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { environment } from '../environments/environment';
import { LoginComponent } from './pages/login/login.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { CarritoComponent } from './pages/carrito/carrito.component';
import { PedidosComponent } from './pages/pedidos/pedidos.component';

// SOLO PRUEBA LOCAL (authBypass=true): sin guards para entrar sin login de Azure.
// Con authBypass=false se protegen las rutas con MsalGuard.
const guards = environment.authBypass ? [] : [MsalGuard];

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'catalogo', component: CatalogoComponent, canActivate: guards },
  { path: 'carrito', component: CarritoComponent, canActivate: guards },
  { path: 'pedidos', component: PedidosComponent, canActivate: guards },
  { path: '**', redirectTo: '' }
];
