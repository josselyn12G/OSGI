// Paquete 'impl': NO está en Export-Package del pom.xml, por lo tanto es invisible
// fuera de este bundle. Esto refuerza el encapsulamiento al estilo OSGi.
package com.example.greeting.impl;

// Importa la interfaz pública que vamos a implementar.
import com.example.greeting.api.GreetingService;

/**
 * Implementación concreta del servicio GreetingService.
 * Permanece como detalle interno del bundle productor: nadie fuera puede instanciarla
 * ni siquiera referenciarla por nombre, porque su paquete no está exportado.
 */
public class GreetingServiceImpl implements GreetingService {

    /**
     * Construye y devuelve el saludo. Lógica de negocio trivial a propósito,
     * porque el foco del ejercicio es la arquitectura modular, no el algoritmo.
     */
    @Override
    public String saludar(String nombre) {
        // Concatena un saludo fijo con el nombre recibido como parámetro.
        return "Hola " + nombre + " desde el bundle de servicio!";
    }
}
