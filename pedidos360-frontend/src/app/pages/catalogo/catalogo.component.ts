import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService } from '../../services/producto.service';
import { CarritoService } from '../../services/carrito.service';
import { Producto } from '../../models/producto.model';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo.component.html'
})
export class CatalogoComponent implements OnInit {

  productos: Producto[] = [];
  productosFiltrados: Producto[] = [];
  categorias: string[] = [];
  categoriaSeleccionada: string | null = null;
  cargando = false;
  error: string | null = null;
  mensajeExito: string | null = null;
  toastVisible = false;

  constructor(
    private productoService: ProductoService,
    private carritoService: CarritoService
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.cargando = true;
    this.error = null;
    this.productoService.listar().subscribe({
      next: (data) => {
        this.productos = data;
        this.categorias = [...new Set(data.map((p) => p.categoria))];
        this.filtrarCategoria(null);
        this.cargando = false;
      },
      error: (err) => {
        this.error = 'No se pudo cargar el catálogo. Verifica que ms-productos esté disponible y que el token sea válido.';
        this.cargando = false;
        console.error(err);
      }
    });
  }

  filtrarCategoria(categoria: string | null): void {
    this.categoriaSeleccionada = categoria;
    this.productosFiltrados = categoria === null
      ? this.productos
      : this.productos.filter((p) => p.categoria === categoria);
  }

  agregarAlCarrito(producto: Producto): void {
    this.mensajeExito = null;
    this.carritoService.agregarItem({
      productoId: producto.id!,
      nombreProducto: producto.nombre,
      precioUnitario: producto.precio,
      cantidad: 1
    }).subscribe({
      next: () => {
        this.mensajeExito = `"${producto.nombre}" se agregó al carrito.`;
        this.toastVisible = true;
        setTimeout(() => (this.toastVisible = false), 3000);
      },
      error: (err) => {
        this.error = 'No se pudo agregar el producto al carrito.';
        console.error(err);
      }
    });
  }
}
