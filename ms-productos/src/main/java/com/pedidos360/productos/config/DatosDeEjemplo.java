package com.pedidos360.productos.config;

import com.pedidos360.productos.model.Producto;
import com.pedidos360.productos.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOLO PARA PRUEBAS LOCALES (app.auth.bypass=true): si la base (H2 en memoria)
 * esta vacia, la carga con productos de ejemplo para poder probar el flujo
 * completo catalogo -> carrito -> checkout -> email.
 *
 * Con AUTH_BYPASS=false (produccion) este bean NO se registra y la BD no se toca.
 */
@Component
@ConditionalOnProperty(name = "app.auth.bypass", havingValue = "true")
public class DatosDeEjemplo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeEjemplo.class);

    private final ProductoRepository productoRepository;

    public DatosDeEjemplo(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productoRepository.count() > 0) {
            return; // ya hay datos (p. ej. reinicio del contenedor con volumes)
        }

        productoRepository.saveAll(List.of(
                new Producto("Laptop Lenovo IdeaPad 3",
                        "Laptop 15\" con Ryzen 5, 8 GB de RAM y SSD de 512 GB. Ideal para estudio y trabajo.",
                        449990.0, 10, "Tecnología"),
                new Producto("Mouse Logitech M170 inalámbrico",
                        "Mouse inalámbrico 2.4 GHz con receptor USB y hasta 10 meses de batería.",
                        14990.0, 25, "Tecnología"),
                new Producto("Teclado mecánico Redragon Kumara",
                        "Teclado mecánico retroiluminado, switches rojos, diseño compacto.",
                        34990.0, 15, "Tecnología"),
                new Producto("Audífonos JBL Tune 520BT",
                        "Audífonos inalámbricos con Bluetooth 5.3 y hasta 57 horas de reproducción.",
                        49990.0, 18, "Tecnología"),
                new Producto("Bicicleta urbana Explorer 21 velocidades",
                        "Bicicleta con marco de acero, frenos de disco y 21 velocidades.",
                        189990.0, 5, "Deportes"),
                new Producto("Balón de fútbol Adidas Starlancer",
                        "Balón tamaño oficial, costuras cosidas para mayor durabilidad.",
                        19990.0, 20, "Deportes"),
                new Producto("Cafetera italiana 6 tazas",
                        "Cafetera moka de aluminio, compatible con todo tipo de cocinas.",
                        24990.0, 12, "Hogar"),
                new Producto("Sartén antiadherente 28 cm",
                        "Sartén con recubrimiento antiadherente y mango ergonómico.",
                        12990.0, 30, "Hogar")
        ));

        log.info("Cargados 8 productos de ejemplo (solo prueba local, AUTH_BYPASS=true)");
    }
}
