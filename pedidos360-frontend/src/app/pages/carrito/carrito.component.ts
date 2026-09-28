import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CarritoService } from '../../services/carrito.service';
import { Carrito } from '../../models/carrito.model';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './carrito.component.html'
})
export class CarritoComponent implements OnInit {

  carrito: Carrito | null = null;
  cargando = false;
  error: string | null = null;
  procesandoCheckout = false;

  constructor(
    private carritoService: CarritoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarCarrito();
  }

  cargarCarrito(): void {
    this.cargando = true;
    this.error = null;
    this.carritoService.obtenerCarrito().subscribe({
      next: (data) => {
        this.carrito = data;
        this.cargando = false;
      },
      error: (err) => {
        this.error = 'No se pudo cargar el carrito.';
        this.cargando = false;
        console.error(err);
      }
    });
  }

  eliminarItem(itemId: number): void {
    this.carritoService.eliminarItem(itemId).subscribe({
      next: (data) => (this.carrito = data),
      error: (err) => {
        this.error = 'No se pudo eliminar el ítem.';
        console.error(err);
      }
    });
  }

  get total(): number {
    if (!this.carrito) return 0;
    return this.carrito.items.reduce((acc, item) => acc + item.precioUnitario * item.cantidad, 0);
  }

  imagenDe(productoId: number | null): string {
    const productos = new Map<number, string>([
      [1, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300&q=80'],
      [2, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300&q=80'],
      [3, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=300&q=80'],
      [4, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300&q=80'],
      [5, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=300&q=80'],
      [6, 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=300&q=80'],
      [7, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=300&q=80'],
      [8, 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=300&q=80']
    ]);
    return productos.get(productoId ?? -1) ?? 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=300&q=80';
  }

  confirmarCompra(): void {
    this.procesandoCheckout = true;
    this.carritoService.checkout().subscribe({
      next: () => {
        this.procesandoCheckout = false;
        this.router.navigate(['/pedidos']);
      },
      error: (err) => {
        this.error = 'No se pudo confirmar la compra. Verifica que el carrito no esté vacío.';
        this.procesandoCheckout = false;
        console.error(err);
      }
    });
  }
}
