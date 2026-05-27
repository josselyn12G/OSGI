# Proyecto OSGi con Apache Karaf — Investigación e Implementación

## 1. ¿Qué es OSGi?

**OSGi (Open Services Gateway initiative)** es una especificación y un framework para Java que define un **sistema modular dinámico**. Permite construir aplicaciones a partir de componentes pequeños, reutilizables y desplegables en caliente llamados **bundles**.

Un **bundle** es un archivo `.jar` enriquecido con metadatos especiales en el `MANIFEST.MF` (cabeceras como `Bundle-SymbolicName`, `Bundle-Version`, `Export-Package`, `Import-Package`, `Bundle-Activator`). Estos metadatos le dicen al framework qué exporta, qué necesita y cómo se debe arrancar/parar.

### Características clave

- **Modularidad real**: cada bundle tiene su propio `ClassLoader` aislado. Solo lo declarado en `Export-Package` es visible desde fuera.
- **Ciclo de vida dinámico**: `INSTALLED → RESOLVED → STARTING → ACTIVE → STOPPING → UNINSTALLED`. Un bundle puede instalarse, arrancarse, pararse, actualizarse o desinstalarse **en caliente, sin reiniciar la JVM**.
- **Versionado de paquetes**: dos bundles pueden depender de versiones diferentes del mismo paquete sin conflicto (algo imposible en un classpath plano).
- **Registro de servicios (Service Registry)**: los bundles publican objetos Java como servicios bajo una interfaz; otros bundles los descubren y consumen sin conocer la implementación concreta.

### Implementaciones del framework
- **Apache Felix** (utilizado dentro de Karaf)
- **Eclipse Equinox** (motor de Eclipse IDE)
- **Knopflerfish**

## 2. Patrones de Arquitectura que utiliza OSGi

OSGi es un caso de estudio porque combina **varios patrones de arquitectura y de diseño**:

### 2.1 Arquitectura Basada en Componentes (Component-Based Architecture)
El sistema se construye ensamblando piezas autocontenidas (bundles). Cada componente puede desarrollarse, probarse y desplegarse de forma independiente.

### 2.2 Arquitectura Orientada a Servicios (SOA — in-process)
OSGi implementa una **SOA dentro de la misma JVM**. Los bundles se comunican únicamente a través de **interfaces de servicio** publicadas en un registro. Esto desacopla al productor del consumidor.

### 2.3 Microkernel / Plug-in Architecture
El framework OSGi (Felix/Equinox) actúa como **microkernel**: un núcleo mínimo responsable solo del ciclo de vida, la resolución de dependencias y el registro de servicios. Toda la funcionalidad de negocio vive en **plug-ins** (bundles) que se cargan sobre él. Eclipse IDE es el ejemplo canónico.

### 2.4 Patrones de diseño internos
- **Service Locator / Registry**: el `BundleContext` actúa como localizador.
- **Observer**: `ServiceListener` y `BundleListener` notifican eventos de aparición/desaparición de servicios o bundles.
- **Whiteboard Pattern**: alternativa al Listener tradicional; los consumidores registran objetos en el registry y el productor los descubre.
- **Dependency Injection**: declarativo mediante **Declarative Services (DS)** o **Blueprint** — los servicios se inyectan sin código boilerplate.

## 3. ¿Qué podemos hacer con OSGi?

- **Aplicaciones modulares de larga vida**: servidores que se actualizan sin downtime (parches en caliente).
- **Sistemas de plug-ins**: IDEs (Eclipse), servidores de aplicaciones (IBM WebSphere, Liferay), pasarelas IoT, sistemas de telecomunicaciones.
- **Aislamiento de versiones**: ejecutar simultáneamente componentes que requieren versiones incompatibles de la misma librería.
- **Despliegue dinámico**: instalar/actualizar/desinstalar funcionalidad en producción a demanda.
- **Bajo acoplamiento**: los bundles solo se conocen a través de contratos (interfaces de servicio).
- **Edge Computing / IoT**: dispositivos con recursos limitados que deben recibir nuevas funcionalidades sin reiniciar.

## 4. Apache Karaf

**Apache Karaf** es un **contenedor OSGi de nivel empresarial** construido sobre Apache Felix (también puede correr sobre Equinox). Aporta sobre el framework desnudo:

- **Consola interactiva (SSH)** con comandos como `bundle:list`, `bundle:install`, `bundle:start`, `bundle:stop`.
- **Hot deploy**: copia un `.jar` a la carpeta `deploy/` y Karaf lo instala y arranca automáticamente.
- **Features**: agrupación de varios bundles en una unidad desplegable mediante un `features.xml`.
- **Gestión de logs, configuración, seguridad y clustering** (Karaf Cellar).

### Pre-requisitos para ejecutar este proyecto
- **JDK 11 u 17** (Karaf 4.4.x soporta ambos)
- **Maven 3.6+**
- **Apache Karaf 4.4.x** — descargar desde https://karaf.apache.org/download.html y descomprimir.

## 5. Estructura del proyecto

```
OSGI/
├── README.md                          ← este archivo
├── pom.xml                            ← POM padre Maven multi-módulo
├── bundle-greeting-service/           ← BUNDLE 1: publica el servicio
│   ├── pom.xml
│   └── src/main/java/com/example/greeting/
│       ├── api/GreetingService.java         ← Interfaz (contrato exportado)
│       ├── impl/GreetingServiceImpl.java    ← Implementación interna
│       └── Activator.java                   ← Registra el servicio al arrancar
└── bundle-greeting-consumer/          ← BUNDLE 2: consume el servicio
    ├── pom.xml
    └── src/main/java/com/example/consumer/
        ├── GreetingConsumer.java            ← Usa el servicio descubierto
        └── Activator.java                   ← Localiza el servicio en el registry
```

### Diseño de bajo acoplamiento
- El **consumidor NO depende de la clase de implementación**. Solo importa el paquete `com.example.greeting.api`.
- El **productor exporta únicamente `com.example.greeting.api`**. La clase `GreetingServiceImpl` está en un paquete `impl` NO exportado → invisible para el consumidor.
- La comunicación se hace 100% a través del **Service Registry de OSGi**.
- Resultado: se puede **detener el bundle productor** y el consumidor recibe un evento `ServiceEvent.UNREGISTERING` y deja de usarlo, sin que la JVM ni el resto del sistema fallen. Al re-arrancarlo, el servicio vuelve a estar disponible.

## 6. Compilación

Desde la carpeta `OSGI/`:

```powershell
mvn clean install
```

Esto genera dos `.jar` (bundles OSGi):
- `bundle-greeting-service/target/bundle-greeting-service-1.0.0.jar`
- `bundle-greeting-consumer/target/bundle-greeting-consumer-1.0.0.jar`

## 7. Despliegue en Apache Karaf

### Paso 1 — Arrancar Karaf
```powershell
cd <KARAF_HOME>\bin
.\karaf.bat
```
Aparecerá la consola `karaf@root()>`.

### Paso 2 — Instalar y arrancar los bundles
Dentro de la consola de Karaf:

```
karaf@root()> bundle:install -s mvn:com.example/bundle-greeting-service/1.0.0
karaf@root()> bundle:install -s mvn:com.example/bundle-greeting-consumer/1.0.0
```

O alternativa simple: copiar los dos `.jar` generados a `<KARAF_HOME>/deploy/`. Karaf los detecta y arranca automáticamente (hot deploy).

### Paso 3 — Listar bundles
```
karaf@root()> bundle:list
```
Verás los dos bundles en estado `Active`.

### Paso 4 — Desinstalar (cuando termines)
```
karaf@root()> bundle:uninstall <ID>
```

## 8. Verificación del funcionamiento — flujo completo con salidas esperadas

Esta sección documenta la prueba real del ejercicio: **parar y arrancar el bundle del servicio a demanda sin afectar al resto del sistema**. Los IDs mostrados (`53` para el service, `54` para el consumer) son los obtenidos en una ejecución de ejemplo; los tuyos pueden variar — usa los que aparezcan en `bundle:list`.

### 8.1. Ver la salida de los `System.out.println`

Depende de cómo se arrancó Karaf:

- **`bin\karaf.bat`** (consola en primer plano) → los mensajes `[SERVICE]` y `[CONSUMER]` aparecen **directamente en esa ventana**.
- **`bin\start.bat`** (Karaf en background) → entrar a la consola con `bin\client.bat` y ejecutar:
  ```
  karaf@root()> log:tail
  ```
  Muestra los mensajes en vivo. `Ctrl+C` para salir del tail (no para Karaf).

Alternativa: revisar el archivo `<KARAF_HOME>\data\log\karaf.log`.

### 8.2. Confirmar que el consumidor encontró al servicio

Tras instalar los dos bundles, en el log debe aparecer una secuencia parecida a:

```
[SERVICE]  Arrancando bundle de servicio: registrando GreetingService...
[SERVICE]  GreetingService registrado y disponible en el registry.
[CONSUMER] Arrancando bundle consumidor...
[CONSUMER] Servicio detectado, saludo: Hola Mundo OSGi desde el bundle de servicio!
```

Si solo aparece `[CONSUMER] Todavía no hay GreetingService disponible.`, significa que el consumidor arrancó antes que el productor. No es un error: el `ServiceListener` lo detectará en cuanto el otro bundle pase a `Active`.

### 8.3. Localizar los IDs de los bundles

```
karaf@root()> bundle:list | grep -i greeting
```

Salida esperada (los IDs pueden variar):
```
ID │ State  │ Lvl │ Version │ Name
53 │ Active │ 80  │ 1.0.0   │ OSGi Karaf Demo - Greeting Service Bundle
54 │ Active │ 80  │ 1.0.0   │ OSGi Karaf Demo - Greeting Consumer Bundle
```

### 8.4. Prueba clave — parar el bundle del servicio sin parar la JVM

```
karaf@root()> bundle:stop 53
```

Salida esperada en el log:
```
[SERVICE]  Parando bundle de servicio: des-registrando GreetingService...
[SERVICE]  GreetingService des-registrado.
[CONSUMER] El servicio GreetingService se fue del registry.
```

👉 Esa última línea del consumidor demuestra que **reaccionó en caliente** vía el `ServiceListener`, sin reiniciar la JVM. El consumidor sigue en estado `Active` (puedes confirmarlo con `bundle:list`).

### 8.5. Volver a arrancar el bundle del servicio

```
karaf@root()> bundle:start 53
```

Salida esperada en el log:
```
[SERVICE]  Arrancando bundle de servicio: registrando GreetingService...
[SERVICE]  GreetingService registrado y disponible en el registry.
[CONSUMER] Servicio detectado, saludo: Hola Mundo OSGi desde el bundle de servicio!
```

👉 El consumidor **redescubre el servicio solo**, sin intervención manual y sin reiniciar nada. Esto es exactamente lo que pide el enunciado: *"poder arrancarse y pararse un bundle a demanda, sin afectar la ejecución del sistema"*.

### 8.6. Diagnóstico opcional del registro de servicios

```
karaf@root()> service:list com.example.greeting.api.GreetingService
```

- Con el service bundle `Active` → aparece la instancia registrada con su `service.id`.
- Con el service bundle `Stopped` → no aparece nada → confirma que el des-registro fue limpio.

### 8.7. Resumen de lo que se demuestra

| Acción                         | Comando                | Reacción del sistema                                              |
|--------------------------------|------------------------|-------------------------------------------------------------------|
| Instalación inicial            | hot deploy / `install` | Productor registra el servicio, consumidor lo descubre y lo usa. |
| Parar el productor             | `bundle:stop 53`       | Consumidor recibe `UNREGISTERING` y deja de usar el servicio.    |
| Re-arrancar el productor       | `bundle:start 53`      | Consumidor recibe `REGISTERED` y vuelve a invocar al servicio.   |
| Estado del consumidor          | siempre `Active`       | Nunca cae, nunca se reinicia.                                    |
| Estado de la JVM               | siempre arriba         | Nunca se reinicia.                                               |

Esto valida los tres requisitos del ejercicio: **dos bundles funcionales, inter-relacionados, con bajo acoplamiento, y con ciclo de vida dinámico independiente**.

## 9. Conclusiones

OSGi materializa los principios de **alta cohesión, bajo acoplamiento, modularidad y dinamismo** dentro de una sola JVM. Combina **microkernel + SOA + componentes** en un único modelo coherente. Apache Karaf agrega la ergonomía operativa (consola, hot deploy, features) que lo vuelve usable en producción. El precio: una curva de aprendizaje sobre `MANIFEST.MF`, classloaders y el ciclo de vida — pero a cambio se obtiene la capacidad real de **actualizar software en caliente sin reiniciar**.
