import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Carrito, AgregarItemRequest } from '../models/carrito.model';
import { Pedido } from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class CarritoService {

  private readonly baseUrl = environment.apiCarritoUrl;

  constructor(private http: HttpClient) {}

  obtenerCarrito(): Observable<Carrito> {
    return this.http.get<Carrito>(`${this.baseUrl}/carrito`);
  }

  agregarItem(request: AgregarItemRequest): Observable<Carrito> {
    return this.http.post<Carrito>(`${this.baseUrl}/carrito/items`, request);
  }

  eliminarItem(itemId: number): Observable<Carrito> {
    return this.http.delete<Carrito>(`${this.baseUrl}/carrito/items/${itemId}`);
  }

  checkout(): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.baseUrl}/carrito/checkout`, {});
  }

  listarPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.baseUrl}/pedidos`);
  }
}
