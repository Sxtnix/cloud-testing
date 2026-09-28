import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html'
})
export class NavbarComponent implements OnInit, OnDestroy {

  isAutenticado = false;
  nombreUsuario: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private msalService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.actualizarEstadoCuenta();

    // SOLO PRUEBA LOCAL (authBypass): no se suscribe a eventos de MSAL.
    if (environment.authBypass) {
      return;
    }

    // Se actualiza cada vez que hay un evento de login/logout exitoso.
    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) =>
          msg.eventType === EventType.LOGIN_SUCCESS || msg.eventType === EventType.LOGOUT_SUCCESS
        ),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.actualizarEstadoCuenta());

    this.msalBroadcastService.inProgress$
      .pipe(
        filter((status) => status === InteractionStatus.None),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.actualizarEstadoCuenta());
  }

  private actualizarEstadoCuenta(): void {
    // SOLO PRUEBA LOCAL (authBypass): usuario simulado, sin cuentas de Azure.
    if (environment.authBypass) {
      this.isAutenticado = true;
      this.nombreUsuario = 'Usuario Local';
      return;
    }
    const cuentas = this.msalService.instance.getAllAccounts();
    this.isAutenticado = cuentas.length > 0;
    this.nombreUsuario = cuentas[0]?.name ?? null;
  }

  login(): void {
    if (environment.authBypass) {
      this.router.navigateByUrl('/catalogo');
      return;
    }
    this.msalService.loginRedirect();
  }

  logout(): void {
    // SOLO PRUEBA LOCAL (authBypass): simplemente se vuelve al inicio.
    if (environment.authBypass) {
      this.router.navigateByUrl('/');
      return;
    }
    this.msalService.logoutRedirect();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
