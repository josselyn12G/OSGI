// Paquete raíz del bundle consumidor (no exportado).
package com.example.consumer;

// API OSGi: ciclo de vida del bundle.
import org.osgi.framework.BundleActivator;
// Contexto OSGi: nuestra puerta hacia el framework (registro, eventos, etc.).
import org.osgi.framework.BundleContext;
// Tipo que representa un PUNTERO a un servicio dentro del registry (sin obtener aún la instancia).
import org.osgi.framework.ServiceReference;
// Evento que el framework dispara cuando un servicio cambia (REGISTERED, MODIFIED, UNREGISTERING).
import org.osgi.framework.ServiceEvent;
// Listener que nos suscribe a esos eventos: clave para reaccionar al dinamismo.
import org.osgi.framework.ServiceListener;

// Interfaz del servicio que queremos consumir (resuelta en runtime contra el bundle productor).
import com.example.greeting.api.GreetingService;

/**
 * Activator del bundle consumidor.
 * Su responsabilidad: localizar el GreetingService cuando exista, dejar de usarlo
 * cuando desaparezca, y volver a tomarlo si reaparece. Todo SIN reiniciar la JVM.
 * Este es el patrón clásico de "consumidor robusto" en OSGi.
 */
public class Activator implements BundleActivator {

    // Guardamos el context para poder hacer lookups y des-suscribirnos en stop().
    private BundleContext context;
    // Listener registrado en el framework; lo guardamos para poder quitarlo en stop().
    private ServiceListener listener;

    /**
     * Llamado por el framework al arrancar el bundle.
     * Estrategia: 1) suscribirnos a eventos para reaccionar al dinamismo;
     *             2) intentar usar el servicio si ya está disponible ahora.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        // Guardamos el contexto para usarlo más adelante (en el listener y en stop()).
        this.context = context;
        System.out.println("[CONSUMER] Arrancando bundle consumidor...");

        // Definimos el listener como expresión lambda (ServiceListener es una @FunctionalInterface).
        // El framework lo llamará cada vez que algún servicio cambie de estado.
        listener = (ServiceEvent evento) -> manejarEvento(evento);

        // Filtro LDAP: solo nos interesan eventos de servicios que implementen GreetingService.
        // Sin este filtro, recibiríamos eventos de TODOS los servicios del framework (ruido).
        String filtro = "(objectClass=" + GreetingService.class.getName() + ")";

        // Registramos el listener. A partir de aquí seremos notificados de cualquier
        // aparición/desaparición de GreetingService durante toda la vida del bundle.
        context.addServiceListener(listener, filtro);

        // Por si el servicio YA está publicado antes de que nosotros arranquemos,
        // hacemos una búsqueda inicial. Sin esto, no detectaríamos un servicio pre-existente.
        usarServicioSiExiste();
    }

    /**
     * Llamado por el framework al parar el bundle. Liberamos todo lo reservado.
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[CONSUMER] Parando bundle consumidor...");
        // Quitamos el listener para no recibir eventos después de parar (evita NPE / fugas).
        if (listener != null) {
            context.removeServiceListener(listener);
            listener = null;
        }
        // Soltamos la referencia al contexto.
        this.context = null;
    }

    /**
     * Maneja un evento del registry filtrado por nuestro tipo de servicio.
     * Aquí se ve materializado el patrón Observer del que se beneficia OSGi.
     */
    private void manejarEvento(ServiceEvent evento) {
        switch (evento.getType()) {
            // El servicio acaba de ser PUBLICADO en el registry → lo usamos.
            case ServiceEvent.REGISTERED:
                usarServicioSiExiste();
                break;
            // El servicio está a punto de ser RETIRADO → reportamos y dejamos de usarlo.
            case ServiceEvent.UNREGISTERING:
                System.out.println("[CONSUMER] El servicio GreetingService se fue del registry.");
                break;
            // Otros tipos (MODIFIED) los ignoramos en este demo.
            default:
                break;
        }
    }

    /**
     * Intenta localizar el servicio en el registry y, si existe, lo invoca.
     * Hace un ciclo completo: get reference → get service → use → unget.
     */
    private void usarServicioSiExiste() {
        // 1) Pedimos al framework un PUNTERO al servicio. null = ninguno publicado.
        ServiceReference<GreetingService> referencia = context.getServiceReference(GreetingService.class);
        if (referencia == null) {
            // Caso normal cuando el bundle productor aún no está arrancado.
            System.out.println("[CONSUMER] Todavía no hay GreetingService disponible.");
            return;
        }

        try {
            // 2) Resolvemos el puntero a la instancia real del servicio.
            GreetingService servicio = context.getService(referencia);
            if (servicio != null) {
                // 3) Usamos el servicio a través de un objeto de negocio dedicado.
                //    Crear la instancia aquí (no en un campo) garantiza que siempre
                //    trabajemos con la versión actual del servicio si fuera reemplazada.
                new GreetingConsumer(servicio).ejecutar();
            }
        } finally {
            // 4) IMPORTANTE: 'ungetService' decrementa el contador de uso para que
            // OSGi pueda liberar el servicio cuando ya nadie lo use. Si no lo hacemos,
            // el framework cree que seguimos consumiéndolo y no podrá limpiarlo bien.
            context.ungetService(referencia);
        }
    }
}
