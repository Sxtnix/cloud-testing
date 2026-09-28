package com.pedidos360.carrito.service;

import com.pedidos360.carrito.dto.AgregarItemRequest;
import com.pedidos360.carrito.model.*;
import com.pedidos360.carrito.repository.CarritoRepository;
import com.pedidos360.carrito.repository.ItemCarritoRepository;
import com.pedidos360.carrito.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final PedidoRepository pedidoRepository;

    public CarritoService(CarritoRepository carritoRepository,
                           ItemCarritoRepository itemCarritoRepository,
                           PedidoRepository pedidoRepository) {
        this.carritoRepository = carritoRepository;
        this.itemCarritoRepository = itemCarritoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Obtiene el carrito activo del usuario. Si no existe, se crea uno nuevo vacío.
     */
    @Transactional
    public Carrito obtenerCarritoActivo(String usuarioId) {
        return carritoRepository.findByUsuarioIdAndEstado(usuarioId, EstadoCarrito.ACTIVO)
                .orElseGet(() -> carritoRepository.save(new Carrito(usuarioId)));
    }

    @Transactional
    public Carrito agregarItem(String usuarioId, AgregarItemRequest request) {
        if (request.cantidad() == null || request.cantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        Carrito carrito = obtenerCarritoActivo(usuarioId);
        ItemCarrito item = new ItemCarrito(
                request.productoId(),
                request.nombreProducto(),
                request.precioUnitario(),
                request.cantidad()
        );
        carrito.agregarItem(item);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito eliminarItem(String usuarioId, Long itemId) {
        Carrito carrito = obtenerCarritoActivo(usuarioId);
        boolean eliminado = carrito.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!eliminado) {
            throw new NoSuchElementException("Item no encontrado en el carrito: " + itemId);
        }
        return carritoRepository.save(carrito);
    }

    /**
     * Convierte el carrito activo en un Pedido confirmado (snapshot de los items y el total),
     * y marca el carrito de origen como FINALIZADO.
     */
    @Transactional
    public Pedido checkout(String usuarioId) {
        Carrito carrito = obtenerCarritoActivo(usuarioId);
        if (carrito.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede confirmar la compra: el carrito está vacío");
        }

        Pedido pedido = new Pedido(usuarioId);
        carrito.getItems().forEach(item -> pedido.agregarItem(ItemPedido.desdeItemCarrito(item)));
        pedido.setTotal(carrito.calcularTotal());

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        carrito.setEstado(EstadoCarrito.FINALIZADO);
        carritoRepository.save(carrito);

        return pedidoGuardado;
    }

    public java.util.List<Pedido> listarPedidos(String usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    public Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado: " + id));
    }
}
