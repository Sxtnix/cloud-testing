import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../../services/carrito.service';
import { Pedido } from '../../models/pedido.model';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pedidos.component.html'
})
export class PedidosComponent implements OnInit {

  pedidos: Pedido[] = [];
  cargando = false;
  error: string | null = null;

  constructor(private carritoService: CarritoService) {}

  ngOnInit(): void {
    this.cargando = true;
    this.carritoService.listarPedidos().subscribe({
      next: (data) => {
        this.pedidos = data;
        this.cargando = false;
      },
      error: (err) => {
        this.error = 'No se pudieron cargar tus pedidos.';
        this.cargando = false;
        console.error(err);
      }
    });
  }
}
