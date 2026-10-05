import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  /** true mientras se redirige a Microsoft: evita dobles clics. */
  iniciando = false;
  error: string | null = null;

  constructor(private msalService: MsalService, private router: Router) {}

  get estaAutenticado(): boolean {
    // SOLO PRUEBA LOCAL (authBypass): se muestra como autenticado sin consultar Azure.
    if (environment.authBypass) {
      return true;
    }
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  login(): void {
    if (this.iniciando) {
      return;
    }
    this.error = null;

    // SOLO PRUEBA LOCAL (authBypass): se entra directo a la tienda sin redirigir a Azure.
    if (environment.authBypass) {
      this.router.navigateByUrl('/catalogo');
      return;
    }

    this.iniciando = true;
    try {
      this.msalService.loginRedirect({ scopes: ['User.Read'] });
      // En caso de fallo sincrono, MSAL lanza excepcion y se muestra el error.
    } catch (err: any) {
      this.iniciando = false;
      this.error = err?.message || 'No se pudo iniciar la sesión con Microsoft. Intenta nuevamente.';
    }
  }
}
