package com.pedidos360.carrito.service;

import com.pedidos360.carrito.dto.AgregarItemRequest;
import com.pedidos360.carrito.model.Carrito;
import com.pedidos360.carrito.model.EstadoCarrito;
import com.pedidos360.carrito.model.EstadoPedido;
import com.pedidos360.carrito.model.Pedido;
import com.pedidos360.carrito.repository.CarritoRepository;
import com.pedidos360.carrito.repository.ItemCarritoRepository;
import com.pedidos360.carrito.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private ItemCarritoRepository itemCarritoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private CarritoService carritoService;

    @Test
    void agregarItem_deberiaCrearCarritoYAgregarItem_siNoExisteCarritoActivo() {
        when(carritoRepository.findByUsuarioIdAndEstado("user-1", EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        AgregarItemRequest request = new AgregarItemRequest(10L, "Pizza", 9990.0, 2);
        Carrito resultado = carritoService.agregarItem("user-1", request);

        assertEquals(1, resultado.getItems().size());
        assertEquals("Pizza", resultado.getItems().get(0).getNombreProducto());
        verify(carritoRepository, atLeastOnce()).save(any(Carrito.class));
    }

    @Test
    void agregarItem_deberiaLanzarExcepcion_siCantidadEsInvalida() {
        AgregarItemRequest request = new AgregarItemRequest(10L, "Pizza", 9990.0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> carritoService.agregarItem("user-1", request));
    }

    @Test
    void checkout_deberiaLanzarExcepcion_siCarritoEstaVacio() {
        Carrito carritoVacio = new Carrito("user-1");
        when(carritoRepository.findByUsuarioIdAndEstado("user-1", EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoVacio));

        assertThrows(IllegalStateException.class, () -> carritoService.checkout("user-1"));
    }

    @Test
    void checkout_deberiaGenerarPedidoConTotalCorrecto() {
        Carrito carrito = new Carrito("user-1");
        carrito.agregarItem(new com.pedidos360.carrito.model.ItemCarrito(1L, "Hamburguesa", 5000.0, 2));
        when(carritoRepository.findByUsuarioIdAndEstado("user-1", EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carrito));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido pedido = carritoService.checkout("user-1");

        assertEquals(10000.0, pedido.getTotal());
        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstado());
        assertEquals(EstadoCarrito.FINALIZADO, carrito.getEstado());
    }
}
