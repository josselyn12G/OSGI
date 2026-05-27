// Paquete del consumidor. No se exporta: este bundle es un cliente final.
package com.example.consumer;

// Importamos SOLO la interfaz pública, nunca la implementación.
// Este es el punto clave del bajo acoplamiento.
import com.example.greeting.api.GreetingService;

/**
 * Clase que encapsula el USO del servicio.
 * La separamos del Activator para mantener la responsabilidad única:
 *   - Activator: gestiona el ciclo de vida y descubrimiento del servicio.
 *   - GreetingConsumer: usa el servicio para hacer trabajo de negocio.
 */
public class GreetingConsumer {

    // Referencia al servicio que vamos a usar. Se asigna desde el Activator.
    private final GreetingService greetingService;

    /**
     * El servicio se INYECTA por constructor (Dependency Injection manual).
     * El consumidor nunca hace 'new GreetingServiceImpl()' — ni siquiera podría,
     * porque la clase Impl está en un paquete no exportado.
     */
    public GreetingConsumer(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * Ejecuta el caso de uso: pide un saludo al servicio y lo imprime por consola
     * de Karaf. En un sistema real esto podría ser exponer un endpoint REST,
     * procesar un mensaje JMS, etc.
     */
    public void ejecutar() {
        // Llamamos al servicio a través de la interfaz; no sabemos ni nos importa
        // quién lo implementa. Si mañana se sustituye Impl por OtraImpl, este código
        // sigue funcionando sin cambios.
        String saludo = greetingService.saludar("Mundo OSGi");
        System.out.println("[CONSUMER] Servicio detectado, saludo: " + saludo);
    }
}
