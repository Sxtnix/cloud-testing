package com.pedidos360.productos.service;

import com.pedidos360.productos.model.Producto;
import com.pedidos360.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void obtenerPorId_deberiaRetornarProducto_siExiste() {
        Producto producto = new Producto("Pizza", "Pizza muzzarella", 9990.0, 10, "Comida");
        producto.setId(1L);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Producto resultado = productoService.obtenerPorId(1L);

        assertEquals("Pizza", resultado.getNombre());
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcion_siNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> productoService.obtenerPorId(99L));
    }

    @Test
    void crear_deberiaGuardarYRetornarProducto() {
        Producto producto = new Producto("Bebida", "Bebida 500ml", 1500.0, 50, "Bebidas");
        when(productoRepository.save(producto)).thenReturn(producto);

        Producto resultado = productoService.crear(producto);

        assertEquals("Bebida", resultado.getNombre());
        verify(productoRepository, times(1)).save(producto);
    }
}
