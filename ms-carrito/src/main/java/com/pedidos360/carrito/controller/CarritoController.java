package com.pedidos360.carrito.controller;

import com.pedidos360.carrito.dto.*;
import com.pedidos360.carrito.model.Carrito;
import com.pedidos360.carrito.model.EstadoPago;
import com.pedidos360.carrito.model.EstadoPedido;
import com.pedidos360.carrito.model.MetodoPago;
import com.pedidos360.carrito.model.Pedido;
import com.pedidos360.carrito.messaging.EmailPublisher;
import com.pedidos360.carrito.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * API del carrito, el checkout, el pago simulado y el historial de pedidos.
 * Todos los recursos estan ligados al usuario autenticado en el JWT.
 */
@RestController
public class CarritoController {

    private final CarritoService carritoService;
    private final EmailPublisher emailPublisher;

    public CarritoController(CarritoService carritoService, EmailPublisher emailPublisher) {
        this.carritoService = carritoService;
        this.emailPublisher = emailPublisher;
    }

    /**
     * Identificador del usuario: el claim "oid" de Azure AD. Si el token no lo
     * trae (p. ej. cuentas invitadas) se usa el "sub" como respaldo.
     */
    private String obtenerUsuarioId(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        if (oid == null || oid.isBlank()) {
            oid = jwt.getSubject();
        }
        if (oid == null || oid.isBlank()) {
            throw new IllegalStateException("El token no incluye un identificador de usuario (oid)");
        }
        return oid;
    }

    // ---------------------------------------------------------------- carrito

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

    @PutMapping("/carrito/items/{itemId}")
    public ResponseEntity<Carrito> actualizarCantidad(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long itemId,
                                                      @RequestBody ActualizarCantidadRequest request) {
        return ResponseEntity.ok(carritoService.actualizarCantidad(
                obtenerUsuarioId(jwt), itemId, request.cantidad()));
    }

    @DeleteMapping("/carrito/items/{itemId}")
    public ResponseEntity<Carrito> eliminarItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(obtenerUsuarioId(jwt), itemId));
    }

    // --------------------------------------------------------------- checkout

    /**
     * Crea el pedido a partir del carrito. El pedido queda PENDIENTE DE PAGO;
     * el pago simulado se procesa en POST /pedidos/{id}/pago.
     */
    @PostMapping("/carrito/checkout")
    public ResponseEntity<Pedido> checkout(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody CheckoutRequest request) {
        MetodoPago metodoPago = MetodoPago.desde(request == null ? null : request.metodoPago());
        Pedido pedido = carritoService.checkout(obtenerUsuarioId(jwt), metodoPago);

        // RabbitMQ -> ms-email envia la confirmacion de compra de forma asincrona.
        emailPublisher.publicarPedidoRegistrado(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                pedido);

        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    /**
     * Pago SIMULADO: delega la decisión en ms-pagos (microservicio separado)
     * y aplica el resultado al pedido. Nunca pide ni almacena datos bancarios
     * reales y el token del usuario se reenvía a ms-pagos para validar el acceso.
     */
    @PostMapping("/pedidos/{id}/pago")
    public ResponseEntity<Pedido> pagar(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) PagoRequest request) {
        boolean rechazoSimulado = request != null && request.esRechazoSimulado();
        String usuarioId = obtenerUsuarioId(jwt);

        Pedido pedido = carritoService.procesarPago(usuarioId, id, rechazoSimulado, jwt.getTokenValue());

        String destino = jwt.getClaimAsString("preferred_username");
        String nombre = jwt.getClaimAsString("name");

        if (pedido.getEstadoPago() == EstadoPago.APROBADO) {
            emailPublisher.publicarPagoAprobado(destino, nombre, pedido);
        } else {
            emailPublisher.publicarPagoRechazado(destino, nombre, pedido);
        }

        return ResponseEntity.ok(pedido);
    }

    // ---------------------------------------------------------------- pedidos

    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> listarPedidos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(carritoService.listarPedidos(obtenerUsuarioId(jwt)));
    }

    @GetMapping("/pedidos/{id}")
    public ResponseEntity<Pedido> obtenerPedido(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(carritoService.obtenerPedido(obtenerUsuarioId(jwt), id));
    }

    /**
     * Avanza un pedido pagado en su seguimiento logistico:
     * CONFIRMADO -> EN_PREPARACION -> ENVIADO -> ENTREGADO.
     * El backend valida la transicion (400 si no esta permitida) y publica
     * el evento CAMBIO_ESTADO para avisar por correo.
     */
    @PutMapping("/pedidos/{id}/estado")
    public ResponseEntity<Pedido> actualizarEstado(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long id,
                                                   @RequestBody ActualizarEstadoRequest request) {
        EstadoPedido nuevoEstado = EstadoPedido.desde(request == null ? null : request.estado());
        Pedido pedido = carritoService.actualizarEstado(obtenerUsuarioId(jwt), id, nuevoEstado);

        emailPublisher.publicarCambioEstado(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                pedido);

        return ResponseEntity.ok(pedido);
    }

    // ------------------------------------------------------------- excepciones

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> manejarNoEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> manejarSinPermiso(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> manejarSolicitudInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
