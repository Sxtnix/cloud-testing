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

  constructor(private msalService: MsalService, private router: Router) {}

  get estaAutenticado(): boolean {
    // SOLO PRUEBA LOCAL (authBypass): se muestra como autenticado sin consultar Azure.
    if (environment.authBypass) {
      return true;
    }
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  login(): void {
    // SOLO PRUEBA LOCAL (authBypass): se entra directo al catálogo sin redirigir a Azure.
    if (environment.authBypass) {
      this.router.navigateByUrl('/catalogo');
      return;
    }
    this.msalService.loginRedirect();
  }
}
