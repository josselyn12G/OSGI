// Paquete raíz del bundle: aquí vive el Activator, que el framework OSGi
// instancia automáticamente al arrancar/parar este bundle.
package com.example.greeting;

// Interfaz que debemos implementar para engancharnos al ciclo de vida del bundle.
import org.osgi.framework.BundleActivator;
// Contexto que el framework nos pasa: es la "puerta" para hablar con OSGi
// (registrar servicios, buscar otros bundles, suscribirse a eventos, etc.).
import org.osgi.framework.BundleContext;
// Tipo devuelto al registrar un servicio; lo guardamos para poder des-registrarlo al parar.
import org.osgi.framework.ServiceRegistration;

// Interfaz pública del servicio (contrato exportado a otros bundles).
import com.example.greeting.api.GreetingService;
// Implementación interna que vamos a publicar bajo la interfaz pública.
import com.example.greeting.impl.GreetingServiceImpl;

/**
 * Activator del bundle productor.
 * Implementando BundleActivator le decimos al framework que llame a start() cuando
 * el bundle pase a ACTIVE y a stop() cuando se le pida pararse. Esto es el corazón
 * del ciclo de vida dinámico de OSGi.
 */
public class Activator implements BundleActivator {

    // Guardamos la referencia al registro para poder anularlo en stop().
    // Sin esto, el servicio seguiría publicado tras parar el bundle (fuga de servicio).
    private ServiceRegistration<GreetingService> registro;

    /**
     * Llamado por el framework cuando el bundle pasa al estado ACTIVE.
     * Aquí publicamos nuestro servicio en el Service Registry para que otros lo descubran.
     */
    @Override
    public void start(BundleContext context) {
        // Mensaje de traza para ver en la consola de Karaf que arrancamos.
        System.out.println("[SERVICE] Arrancando bundle de servicio: registrando GreetingService...");

        // Instanciamos la implementación concreta. Solo este bundle puede hacer este 'new'
        // porque solo él ve la clase Impl (paquete no exportado).
        GreetingServiceImpl implementacion = new GreetingServiceImpl();

        // Publicamos la instancia en el registro OSGi:
        //   1er arg: la INTERFAZ bajo la que se registra (contrato visible para todos).
        //   2do arg: la INSTANCIA real que atenderá las llamadas.
        //   3er arg: properties opcionales (rank, filtros, etc.). null = ninguna.
        // Cualquier bundle que pregunte por GreetingService recibirá esta instancia.
        registro = context.registerService(GreetingService.class, implementacion, null);

        // Traza final indicando que el servicio quedó disponible.
        System.out.println("[SERVICE] GreetingService registrado y disponible en el registry.");
    }

    /**
     * Llamado por el framework cuando se pide parar el bundle.
     * Aquí debemos LIBERAR todo lo que reservamos en start() para evitar fugas.
     */
    @Override
    public void stop(BundleContext context) {
        System.out.println("[SERVICE] Parando bundle de servicio: des-registrando GreetingService...");
        // Anular el registro dispara un ServiceEvent.UNREGISTERING que el consumidor escucha;
        // así el consumidor se entera al instante de que el servicio ya no está.
        if (registro != null) {
            registro.unregister();
            // Anulamos la referencia local para que el GC libere los recursos.
            registro = null;
        }
        System.out.println("[SERVICE] GreetingService des-registrado.");
    }
}
