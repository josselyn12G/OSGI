// Declaración del paquete: este es el ÚNICO paquete exportado por el bundle productor.
// Eso significa que solo lo que viva aquí será visible para otros bundles.
package com.example.greeting.api;

/**
 * Interfaz que define el CONTRATO público del servicio de saludo.
 * Es lo que el consumidor verá; la implementación concreta queda oculta.
 * Trabajar contra una interfaz, y no contra una clase, es lo que produce el BAJO ACOPLAMIENTO.
 */
public interface GreetingService {

    /**
     * Devuelve un saludo personalizado.
     * @param nombre nombre del destinatario del saludo.
     * @return cadena con el saludo construido.
     */
    String saludar(String nombre);
}
