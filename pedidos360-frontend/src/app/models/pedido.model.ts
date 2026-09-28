export interface ItemPedido {
  id: number;
  productoId: number;
  nombreProducto: string;
  precioUnitario: number;
  cantidad: number;
}

export interface Pedido {
  id: number;
  usuarioId: string;
  total: number;
  estado: string;
  fecha: string;
  items: ItemPedido[];
}
