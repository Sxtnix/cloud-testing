package com.pedidos360.carrito.controller;

import com.pedidos360.carrito.dto.AgregarItemRequest;
import com.pedidos360.carrito.model.Carrito;
import com.pedidos360.carrito.model.Pedido;
import com.pedidos360.carrito.messaging.EmailPublisher;
import com.pedidos360.carrito.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
public class CarritoController {

    private final CarritoService carritoService;
    private final EmailPublisher emailPublisher;

    public CarritoController(CarritoService carritoService, EmailPublisher emailPublisher) {
        this.carritoService = carritoService;
        this.emailPublisher = emailPublisher;
    }

    private String obtenerUsuarioId(Jwt jwt) {
        return jwt.getClaimAsString("oid");
    }

    @GetMapping("/carrito")
    public ResponseEntity<Carrito> obtenerCarrito(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carritoService.obtenerCarritoActivo(obtenerUsuarioId(jwt)));
    }

    @PostMapping("/carrito/items")
    public ResponseEntity<Carrito> agregarItem(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody AgregarItemRequest request) {
        Carrito carrito = carritoService.agregarItem(obtenerUsuarioId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(carrito);
    }

    @DeleteMapping("/carrito/items/{itemId}")
    public ResponseEntity<Carrito> eliminarItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(obtenerUsuarioId(jwt), itemId));
    }

    @PostMapping("/carrito/checkout")
    public ResponseEntity<Pedido> checkout(@AuthenticationPrincipal Jwt jwt) {
        Pedido pedido = carritoService.checkout(obtenerUsuarioId(jwt));

        // Publica la confirmación en RabbitMQ: ms-email la consume y envía el correo.
        emailPublisher.publicarPedidoConfirmado(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                pedido);

        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> listarPedidos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carritoService.listarPedidos(obtenerUsuarioId(jwt)));
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(carritoService.obtenerPedido(id));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> manejarNoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> manejarSolicitudInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
