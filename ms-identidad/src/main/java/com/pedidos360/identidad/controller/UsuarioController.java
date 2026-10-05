package com.pedidos360.identidad.controller;

import com.pedidos360.identidad.dto.UsuarioDTO;
import com.pedidos360.identidad.model.RolUsuario;
import com.pedidos360.identidad.model.Usuario;
import com.pedidos360.identidad.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Devuelve (y crea si no existe) el perfil del usuario autenticado,
     * usando los claims del JWT emitido por Azure AD.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> obtenerPerfilActual(@AuthenticationPrincipal Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        String nombre = jwt.getClaimAsString("name");
        String email = jwt.getClaimAsString("preferred_username");

        Usuario usuario = usuarioService.obtenerOCrearPorOid(oid, nombre, email);
        return ResponseEntity.ok(new UsuarioDTO(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarTodos().stream()
                .map(UsuarioDTO::new)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(new UsuarioDTO(usuarioService.obtenerPorId(id)));
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioDTO> actualizarRol(@PathVariable Long id, @RequestBody ActualizarRolRequest request) {
        Usuario actualizado = usuarioService.actualizarRol(id, request.rol());
        return ResponseEntity.ok(new UsuarioDTO(actualizado));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<String> manejarNoEncontrado(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    public record ActualizarRolRequest(RolUsuario rol) {
    }
}
