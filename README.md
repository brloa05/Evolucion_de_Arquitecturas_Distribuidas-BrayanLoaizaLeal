# Taller Integrador ARSW 2026-I
## Evolución de Arquitecturas Distribuidas con Java

**Autor:** Brayan Loaiza Leal  
**Curso:** Arquitecturas de Software (ARSW) — Escuela Colombiana de Ingeniería Julio Garavito  
**Periodo:** 2026-I

---

## ¿De qué trata este proyecto?

Este taller implementa **el mismo dominio de negocio** (películas y bienestar universitario) usando **6 estilos arquitectónicos distintos**, evolucionando desde la comunicación más primitiva (sockets TCP) hasta un API Gateway moderno sobre microservicios gRPC. El objetivo es entender qué problema resuelve cada arquitectura y qué nueva complejidad introduce.

```
TCP Sockets ──► HTTP ──► RMI ──► gRPC ──► Microservicios ──► API Gateway
    I            II      III      IV            V                  VI
```

| Parte | Arquitectura      | Tecnología                    | Puerto(s)                    |
|-------|-------------------|-------------------------------|------------------------------|
| I     | TCP Sockets       | `ServerSocket` / `Socket`     | 35000 / 31000                |
| II    | HTTP              | `com.sun.net.httpserver`      | 8080 / 8081                  |
| III   | RMI               | `java.rmi`                    | 23000 / 24000                |
| IV    | gRPC              | Protocol Buffers + HTTP/2     | 50051 / 50052                |
| V     | Microservicios    | gRPC independientes           | 50055 / 50056 / 50060 / 50061|
| VI    | API Gateway       | HTTP fachada sobre gRPC       | 8090 / 8091 / 8095           |

---

## Requisitos

- Java 17+
- Maven 3.8+
- `protoc` (descargado automáticamente por `protobuf-maven-plugin`)

## Compilar

```bash
mvn compile
```

---

## Parte I — TCP Sockets

### ¿Qué se aprende?

TCP Sockets es la base de toda comunicación en red. En esta parte el servidor abre un `ServerSocket` en un puerto, espera clientes, y se comunica con ellos mediante texto plano a través de `BufferedReader` / `PrintWriter`. No existe ningún protocolo estándar: el programador define el formato de los mensajes. Es la arquitectura más simple pero también la más frágil: cualquier cambio en el formato rompe la comunicación.

---

### Ejemplo — Movie TCP Server

**Qué hace:** El servidor mantiene un catálogo de 3 películas en memoria. El cliente envía un comando de texto y el servidor responde con la información de la película.

**Cómo funciona:**
1. `MovieServer` abre un `ServerSocket` en el puerto **35000** y entra en un bucle infinito esperando conexiones.
2. Por cada cliente, lee líneas de texto e interpreta el comando `get movie <id>`.
3. Busca la película en `MovieRepository` y responde con sus datos en texto plano.
4. `MovieClient` establece la conexión, envía el comando y muestra la respuesta.

**Películas disponibles:** `id=1` Interstellar, `id=2` The Matrix, `id=3` Inception

| Clase | Descripción |
|-------|-------------|
| `movie_tcp/MovieServer.java` | Servidor TCP — puerto **35000** |
| `movie_tcp/MovieClient.java` | Cliente TCP |
| `movie_tcp/Movie.java` | Modelo de datos |
| `movie_tcp/MovieRepository.java` | Repositorio en memoria (3 películas) |

**Ejecutar:**
```bash
# Terminal 1 — Servidor
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_tcp.MovieServer"

# Terminal 2 — Cliente
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_tcp.MovieClient"
```

**Protocolo de comunicación:**
```
Cliente  →  get movie 1
Servidor →  id:1|title:Interstellar|director:Christopher Nolan|year:2014
```

---

### Ejercicio — Room TCP Server

**Qué hace:** Gestión de salas de reunión universitarias. Permite listar todas las salas, consultar una sala específica, reservarla y liberarla, todo a través de un protocolo de texto plano sobre TCP.

**Cómo funciona:**
1. `RoomServer` escucha en el puerto **31000**. Mantiene un `Map<Integer, Room>` con 5 salas precargadas.
2. El cliente `RoomClient` muestra un menú interactivo con 4 opciones.
3. Al elegir una opción, el cliente construye el texto del comando y lo envía al servidor.
4. El servidor parsea el comando, ejecuta la operación sobre `RoomRepository` y responde con el resultado.
5. Las reservas cambian el estado `isReserved` de la sala. Si está reservada, no se puede volver a reservar.

**Salas disponibles:** `id=1` Sala A · `id=2` Sala B · `id=3` Sala C · `id=4` Sala D · `id=5` Sala E

| Clase | Descripción |
|-------|-------------|
| `ejercicio_Evolution/RoomServer.java` | Servidor TCP — puerto **31000** |
| `ejercicio_Evolution/RoomClient.java` | Cliente con menú interactivo |
| `ejercicio_Evolution/Room.java` | Modelo: id, nombre, capacidad, reservada |
| `ejercicio_Evolution/RoomRepository.java` | Repositorio con operaciones CRUD + reserva |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_Evolution.RoomServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_Evolution.RoomClient"
```

**Protocolo de comunicación:**
```
get rooms           → devuelve todas las salas
get room 1          → devuelve detalle de la sala 1
reserve room 1      → reserva la sala 1 (error si ya está reservada)
release room 1      → libera la sala 1 (error si no estaba reservada)
```

---

## Parte II — HTTP

### ¿Qué se aprende?

HTTP introduce el primer **contrato estándar** de comunicación: verbos (`GET`, `POST`), rutas (`/rooms`), query params (`?id=1`) y códigos de estado (`200 OK`, `404 Not Found`). A diferencia de TCP puro, cualquier cliente HTTP (navegador, curl, Postman) puede consumir el servidor sin necesidad de conocer el protocolo propietario. Se usa la librería `com.sun.net.httpserver` incluida en el JDK, sin frameworks externos.

---

### Ejemplo — Movie HTTP Server

**Qué hace:** Expone el mismo catálogo de películas de la Parte I a través de una API HTTP. Ahora el cliente puede ser un navegador web.

**Cómo funciona:**
1. `MovieHttpServer` crea un `HttpServer` en el puerto **8080**.
2. Registra un `HttpHandler` para la ruta `/movie`.
3. Al recibir una petición `GET /movie?id=1`, extrae el parámetro `id`, consulta `MovieRepository` (el mismo de la Parte I) y responde con los datos de la película en texto plano.
4. Si el id no existe responde con `404 Not Found`.

| Clase | Descripción |
|-------|-------------|
| `MovieHttpServer/MovieHttpServer.java` | Servidor HTTP — puerto **8080** |

**Ejecutar:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.MovieHttpServer.MovieHttpServer"
```

**Probar en navegador o curl:**
```bash
curl "http://localhost:8080/movie?id=1"
curl "http://localhost:8080/movie?id=99"   # → 404
```

---

### Ejercicio — Room HTTP Server

**Qué hace:** Convierte el servidor de salas de la Parte I en una API REST completa con 4 endpoints. Introduce el concepto de recursos HTTP con operaciones `GET` (lectura) y `POST` (escritura/modificación).

**Cómo funciona:**
1. `RoomHttpServer` crea un `HttpServer` en el puerto **8081**.
2. Registra tres `HttpHandler` anidados como clases internas: `RoomsHandler`, `ReserveHandler` y `ReleaseHandler`.
3. `RoomsHandler` atiende `GET /rooms` (lista) y `GET /rooms?id=X` (detalle individual).
4. `ReserveHandler` atiende `POST /rooms/reserve?id=X` y cambia el estado de la sala.
5. `ReleaseHandler` atiende `POST /rooms/release?id=X` y revierte el estado.
6. Reutiliza `RoomRepository` de la Parte I — el estado persiste en memoria mientras el servidor esté activo.

| Clase | Descripción |
|-------|-------------|
| `ejercicio_http/RoomHttpServer.java` | Servidor HTTP — puerto **8081**, 3 handlers internos |

**Ejecutar:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_http.RoomHttpServer"
```

**Probar con curl:**
```bash
# Listar todas las salas
curl http://localhost:8081/rooms

# Ver sala específica
curl "http://localhost:8081/rooms?id=1"

# Reservar sala
curl -X POST "http://localhost:8081/rooms/reserve?id=1"

# Liberar sala
curl -X POST "http://localhost:8081/rooms/release?id=1"
```

---

## Parte III — RMI (Remote Method Invocation)

### ¿Qué se aprende?

RMI lleva la abstracción un paso más allá: el cliente llama **métodos Java directamente** sobre un objeto remoto, como si fuera un objeto local. No hay texto que parsear ni rutas que definir. La serialización de objetos Java se maneja automáticamente. El precio es el acoplamiento total a la JVM: cliente y servidor deben estar escritos en Java y compartir las interfaces. Un `RMIRegistry` actúa como directorio para localizar el servicio por nombre.

---

### Ejemplo — Movie RMI

**Qué hace:** El mismo catálogo de películas, ahora accesible como si fuera una llamada a un método Java local: `movieService.getMovie(1)`.

**Cómo funciona:**
1. `MovieService` es una interfaz que extiende `Remote`. Define los métodos que se pueden invocar remotamente.
2. `MovieServiceImpl` extiende `UnicastRemoteObject` e implementa la lógica.
3. `MovieRmiServer` registra la instancia en el `RMIRegistry` con el nombre `"MovieService"` en el puerto **23000**.
4. `MovieRmiClient` localiza el registro, hace `lookup("MovieService")` y obtiene un stub del objeto remoto. Llama `getMovie(id)` como si fuera local.
5. `Movie` implementa `Serializable` para poder viajar por la red entre JVMs.

| Clase | Descripción |
|-------|-------------|
| `movie_rmi/MovieRmiServer.java` | Crea el registry y registra el servicio — puerto **23000** |
| `movie_rmi/MovieRmiClient.java` | Busca el servicio en el registry y llama métodos remotos |
| `movie_rmi/MovieService.java` | Interfaz remota (`extends Remote`) |
| `movie_rmi/MovieServiceImpl.java` | Implementación (`extends UnicastRemoteObject`) |
| `movie_rmi/Movie.java` | Objeto de dominio (`implements Serializable`) |

**Ejecutar:**
```bash
# Terminal 1 — Servidor
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_rmi.MovieRmiServer"

# Terminal 2 — Cliente
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_rmi.MovieRmiClient"
```

---

### Ejercicio — Lab Inventory RMI

**Qué hace:** Sistema de inventario de equipos de laboratorio. Permite consultar todos los equipos, ver un equipo por código, reservarlo y liberarlo, usando invocación remota de métodos.

**Cómo funciona:**
1. `LabService` define 4 métodos remotos: `consultarEquipos()`, `consultarEquipo(String)`, `reservarEquipo(String)`, `liberarEquipo(String)`.
2. `LabServiceImpl` mantiene 5 equipos en un `Map<String, Equipment>`. Las operaciones de reserva/liberación cambian el campo `available` del equipo.
3. `LabRmiServer` registra la implementación en el puerto **24000** con el nombre `"LabService"`.
4. `LabRmiClient` presenta un menú de 4 opciones. Al elegir una, llama el método correspondiente sobre el stub remoto. El objeto `Equipment` viaja serializado desde el servidor.

**Equipos precargados:**

| Código | Nombre | Laboratorio |
|--------|--------|-------------|
| PC-001 | Computador Dell | Lab Sistemas |
| PC-002 | Computador HP | Lab Sistemas |
| AR-001 | Visor HoloLens | Lab AR/VR |
| AR-002 | Cámara 360° | Lab AR/VR |
| OS-001 | Raspberry Pi | Lab IoT |

| Clase | Descripción |
|-------|-------------|
| `ejercicio_rmi/LabRmiServer.java` | Registry + servidor — puerto **24000** |
| `ejercicio_rmi/LabRmiClient.java` | Menú interactivo con 4 opciones |
| `ejercicio_rmi/LabService.java` | Interfaz remota con los 4 métodos |
| `ejercicio_rmi/LabServiceImpl.java` | Implementación con los 5 equipos precargados |
| `ejercicio_rmi/Equipment.java` | Modelo: código, nombre, laboratorio, disponible |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_rmi.LabRmiServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_rmi.LabRmiClient"
```

**Menú del cliente:**
```
1. Ver todos los equipos
2. Consultar equipo por código (ej: PC-001)
3. Reservar equipo
4. Liberar equipo
```

---

## Parte IV — gRPC

### ¿Qué se aprende?

gRPC combina lo mejor de RMI (llamadas a métodos tipados) con la independencia de lenguaje de HTTP. Los contratos se definen en archivos `.proto` (Protocol Buffers), que el compilador `protoc` convierte automáticamente en clases Java (stubs, builders, etc.). La comunicación usa HTTP/2, lo que permite multiplexación y compresión binaria, haciendo gRPC mucho más eficiente que REST/JSON. Si se cambia el `.proto`, el compilador detecta incompatibilidades en tiempo de compilación.

---

### Ejemplo — Movie gRPC

**Qué hace:** El catálogo de películas expuesto como servicio gRPC. El cliente usa un stub generado automáticamente para hacer llamadas tipadas.

**Cómo funciona:**
1. `movie.proto` define el contrato: un servicio con un método `GetMovie` que recibe un `MovieRequest` (con `id`) y retorna un `MovieResponse` (con `title`, `director`, `year`, `found`).
2. `mvn compile` ejecuta `protoc`, que genera las clases `MovieServiceGrpc`, `MovieRequest`, `MovieResponse`, etc. en el directorio `target/`.
3. `MovieGrpcServer` extiende `MovieServiceGrpc.MovieServiceImplBase` y sobreescribe `getMovie()`. Usa `ServerBuilder` para arrancar en el puerto **50051**.
4. `MovieGrpcClient` crea un `ManagedChannel` hacia `localhost:50051` y obtiene un stub bloqueante. Llama `stub.getMovie(request)` y procesa la respuesta tipada.

**Contrato `movie.proto`:**
```protobuf
service MovieService {
  rpc GetMovie (MovieRequest) returns (MovieResponse);
}
message MovieRequest  { int32 id = 1; }
message MovieResponse { int32 id = 1; string title = 2; string director = 3; int32 year = 4; bool found = 5; }
```

| Clase | Descripción |
|-------|-------------|
| `movie_grpc/MovieGrpcServer.java` | Servidor gRPC — puerto **50051** |
| `movie_grpc/MovieGrpcClient.java` | Cliente con stub bloqueante |
| `src/main/proto/movie.proto` | Contrato del servicio |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcClient"
```

---

### Ejercicio — Appointment gRPC (Bienestar Universitario)

**Qué hace:** Sistema completo de citas médicas universitarias con 3 tipos de servicio (Medicina, Psicología, Odontología). Permite solicitar citas, cancelarlas y consultar el historial de un estudiante.

**Cómo funciona:**
1. `appointment.proto` define un `enum ServiceType` (MEDICINE, PSYCHOLOGY, DENTISTRY), un `enum AppointmentStatus` (REQUESTED, CANCELLED, ATTENDED) y 3 métodos RPC.
2. `AppointmentGrpcServer` implementa los 3 métodos. Al solicitar una cita genera un UUID como ID, crea un objeto `Appointment` y lo guarda en una lista en memoria.
3. Al cancelar, busca la cita por ID y cambia su estado a `CANCELLED`.
4. Al consultar por estudiante, filtra la lista por `studentId` y devuelve todas sus citas.
5. `AppointmentGrpcClient` muestra un menú interactivo para todas las operaciones.

**Contrato `appointment.proto`:**
```protobuf
service AppointmentService {
  rpc RequestAppointment (AppointmentRequest) returns (AppointmentResponse);
  rpc CancelAppointment  (CancelRequest)      returns (CancelResponse);
  rpc GetAppointments    (StudentRequest)      returns (AppointmentList);
}
```

| Clase | Descripción |
|-------|-------------|
| `ejercicio_grpc/AppointmentGrpcServer.java` | Servidor gRPC — puerto **50052** |
| `ejercicio_grpc/AppointmentGrpcClient.java` | Menú interactivo: solicitar, cancelar, consultar |
| `src/main/proto/appointment.proto` | Contrato con enums, mensajes y 3 RPCs |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcClient"
```

**Menú del cliente:**
```
1. Solicitar cita  → ingresa ID estudiante, nombre, email, tipo (0/1/2), fecha
2. Cancelar cita   → ingresa ID de la cita
3. Ver mis citas   → ingresa ID del estudiante
```

---

## Parte V — Microservicios

### ¿Qué se aprende?

En la Parte IV un solo servidor hace todo. En microservicios, cada responsabilidad se divide en un **proceso independiente** con su propio puerto, su propio `.proto` y su propio ciclo de despliegue. Esto permite escalar solo el servicio que tiene carga, actualizar un servicio sin tocar los demás, y que distintos equipos trabajen en paralelo. El cliente agrega los resultados de múltiples servicios en una sola respuesta.

---

### Ejemplo — Microservicios de Películas

**Qué hace:** Divide la información de películas en 3 servicios independientes. `MovieService` da los datos básicos, `ReviewService` da las reseñas, y `RecommendationService` da las recomendaciones. El cliente los consulta a todos y presenta una vista unificada.

**Cómo funciona:**
1. `ReviewGrpcServer` (puerto **50055**) implementa `ReviewService` definido en `review.proto`. Tiene reseñas hardcodeadas para las películas 1, 2 y 3.
2. `RecommendationGrpcServer` (puerto **50056**) implementa `RecommendationService` de `recommendation.proto`. Tiene listas de títulos recomendados por película.
3. `MovieMicroClient` abre 3 canales gRPC (a los puertos 50051, 50055, 50056), pide el ID al usuario, hace 3 llamadas paralelas y presenta toda la información junta.
4. `MovieGrpcServer` (50051, de la Parte IV) se reutiliza sin modificación.

**Contratos:**
```protobuf
// review.proto
service ReviewService {
  rpc GetReviews (ReviewRequest) returns (ReviewList);
}
// recommendation.proto
service RecommendationService {
  rpc GetRecommendations (RecommendationRequest) returns (RecommendationList);
}
```

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `movie_microservices/ReviewGrpcServer.java` | **50055** | Reseñas: autor, comentario, rating (1-5) |
| `movie_microservices/RecommendationGrpcServer.java` | **50056** | Recomendaciones: lista de títulos por película |
| `movie_microservices/MovieMicroClient.java` | — | Agrega los 3 servicios en una sola vista |

**Ejecutar (4 terminales):**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.ReviewGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.RecommendationGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.MovieMicroClient"
```

---

### Ejercicio — Microservicios de Bienestar

**Qué hace:** Divide el sistema de bienestar universitario en 3 servicios especializados. `AppointmentService` gestiona citas (Parte IV reutilizado), `MedicalService` expone especialidades médicas y `GymService` gestiona franjas del gimnasio con reserva en tiempo real.

**Cómo funciona:**
1. `MedicalGrpcServer` (puerto **50060**) implementa `MedicalService` de `medical.proto`. Mantiene 3 especialidades en memoria con su nombre, cupos disponibles y horario. Soporta listar todas las especialidades o consultar una por nombre.
2. `GymGrpcServer` (puerto **50061**) implementa `GymService` de `gym.proto`. Mantiene un `Map<String, Boolean>` con 5 franjas horarias (la clave es la franja, el valor indica si está libre). Al reservar, verifica disponibilidad y cambia el estado a `false`.
3. `WellnessClient` conecta a los 3 servicios simultáneamente y presenta un menú unificado de 4 opciones.

**Contratos:**
```protobuf
// medical.proto
service MedicalService {
  rpc GetSpecialties   (EmptyRequest)     returns (SpecialtyList);
  rpc GetSpecialtyInfo (SpecialtyRequest) returns (SpecialtyInfo);
}
// gym.proto
service GymService {
  rpc GetAvailableSlots (EmptyGymRequest) returns (SlotList);
  rpc ReserveSlot       (SlotRequest)     returns (SlotResponse);
}
```

**Datos precargados:**

| Especialidad | Cupos | Horario |
|--------------|-------|---------|
| MEDICINE | 5 | Lunes-Viernes 8am-12pm |
| PSYCHOLOGY | 3 | Martes-Jueves 2pm-5pm |
| DENTISTRY | 2 | Miércoles 9am-11am |

| Franja Gimnasio | Disponible |
|-----------------|------------|
| 07:00 - 08:00 | Sí |
| 08:00 - 09:00 | Sí |
| 12:00 - 13:00 | Sí |
| 17:00 - 18:00 | Sí |
| 18:00 - 19:00 | Sí |

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `ejercicio_microservices/MedicalGrpcServer.java` | **50060** | Especialidades con cupos y horarios |
| `ejercicio_microservices/GymGrpcServer.java` | **50061** | Franjas del gimnasio con estado de reserva |
| `ejercicio_microservices/WellnessClient.java` | — | Menú que agrega Appointment + Medical + Gym |

**Ejecutar (4 terminales):**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.MedicalGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.GymGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.WellnessClient"
```

---

## Parte VI — API Gateway

### ¿Qué se aprende?

Con microservicios, el cliente necesita conocer la dirección de cada servicio. El **API Gateway** es una capa intermedia que actúa como punto único de entrada: el cliente solo habla con el gateway (HTTP), y este se encarga de enrutar las peticiones a los microservicios internos (gRPC). Centraliza autenticación, logging y transformación de formatos. El cliente no sabe cuántos servicios existen ni cómo se comunican entre sí.

```
Cliente HTTP  ──►  API Gateway  ──►  Microservicio A (gRPC)
                       │         ──►  Microservicio B (gRPC)
                       │         ──►  Microservicio C (gRPC)
```

---

### Ejemplo — MovieGateway (puerto 8090)

**Qué hace:** Expone un único endpoint HTTP que internamente llama a los 3 microservicios de películas (MovieService, ReviewService, RecommendationService) y devuelve una respuesta JSON unificada. El cliente HTTP no sabe que existen 3 servicios gRPC.

**Cómo funciona:**
1. `MovieGateway` crea un `HttpServer` en el puerto **8090**.
2. Al recibir `GET /api/movie?id=X`, crea 3 stubs gRPC bloqueantes apuntando a los puertos 50051, 50055 y 50056.
3. Hace las 3 llamadas gRPC secuencialmente, recoge las respuestas y construye un JSON que combina película + reseñas + recomendaciones.
4. Responde con `Content-Type: application/json`.

> **Importante:** Los 3 microservicios deben estar corriendo antes de iniciar el gateway.

| Clase | Descripción |
|-------|-------------|
| `movie_gateway/MovieGateway.java` | Gateway HTTP — puerto **8090** |

**Iniciar microservicios primero:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.ReviewGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.RecommendationGrpcServer"
```

**Iniciar el gateway:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_gateway.MovieGateway"
```

**Probar:**
```bash
curl "http://localhost:8090/api/movie?id=1"
```

**Respuesta JSON:**
```json
{
  "id": 1,
  "title": "Interstellar",
  "director": "Christopher Nolan",
  "year": 2014,
  "reviews": [
    { "author": "Alice", "comment": "Excelente pelicula de ciencia ficcion", "rating": 5 },
    { "author": "Bob",   "comment": "Visualmente impresionante",             "rating": 4 }
  ],
  "recommendations": ["Inception", "Contact", "2001: A Space Odyssey"]
}
```

---

### Ejercicio — WellnessGateway (puerto 8091)

**Qué hace:** Gateway HTTP para el sistema de bienestar que agrega AppointmentService, MedicalService y GymService. Expone 4 endpoints REST, cada uno delegando internamente a su microservicio gRPC correspondiente.

**Cómo funciona:**
1. `WellnessGateway` crea un `HttpServer` en el puerto **8091** con 4 contextos (rutas).
2. `GET /api/wellness/specialties` llama a `MedicalService.getSpecialties()` y serializa la respuesta a JSON.
3. `GET /api/wellness/gym/slots` llama a `GymService.getAvailableSlots()` y retorna la lista de franjas libres.
4. `POST /api/wellness/gym/reserve` lee los query params `studentId` y `slot`, llama a `GymService.reserveSlot()` y responde `200` si tuvo éxito o `409` si la franja ya estaba ocupada.
5. `POST /api/wellness/appointment` recibe los datos del estudiante y llama a `AppointmentService.requestAppointment()`.

> **Importante:** Los 3 microservicios deben estar corriendo antes de iniciar el gateway.

| Clase | Descripción |
|-------|-------------|
| `wellness_gateway/WellnessGateway.java` | Gateway HTTP — puerto **8091**, 4 endpoints |

**Iniciar microservicios primero:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.MedicalGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.GymGrpcServer"
```

**Iniciar el gateway:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.wellness_gateway.WellnessGateway"
```

**Probar:**
```bash
# Ver especialidades médicas
curl http://localhost:8091/api/wellness/specialties

# Ver franjas del gimnasio disponibles
curl http://localhost:8091/api/wellness/gym/slots

# Reservar una franja
curl -X POST "http://localhost:8091/api/wellness/gym/reserve?studentId=EST-001&slot=07:00+-+08:00"

# Solicitar cita médica
curl -X POST "http://localhost:8091/api/wellness/appointment?studentId=EST-001&name=Ana&email=ana@eci.edu.co&type=0&date=2026-06-20"
```

---

## Ejercicio Final — Plataforma ECICIENCIA

### ¿Qué es?

ECICIENCIA es una plataforma académica integral diseñada para la ECI, que integra gestión de estudiantes, inscripción a cursos y préstamo de libros en una única API. Aplica la arquitectura de **microservicios gRPC + API Gateway HTTP** sobre un dominio completamente nuevo, consolidando todos los conceptos del taller.

### Arquitectura completa

```
                        ┌────────────────────────────┐
                        │  Cliente (navegador/curl)  │
                        │   HTTP → localhost:8095    │
                        └──────────────┬─────────────┘
                                       │
                        ┌─────────────▼──────────────┐
                        │      ECICIENCIAGateway      │
                        │  (único punto de entrada)   │
                        └──────┬─────────┬────────────┘
                               │         │         │
                         gRPC  │   gRPC  │   gRPC  │
                        :50070 │  :50071 │  :50072 │
                  ┌───────────┐│┌────────┐│┌───────────┐
                  │  Student  │││ Course │││  Library  │
                  │  Service  │││Service │││  Service  │
                  └───────────┘│└────────┘│└───────────┘
                               │          │
                    Estudiantes │  Cursos  │  Libros
                    + registro  │ + matrícula│+ búsqueda y préstamo
```

### Contratos Proto

**`student.proto`:**
```protobuf
service StudentService {
  rpc GetStudent      (GetStudentRequest) returns (StudentResponse);
  rpc RegisterStudent (RegisterRequest)   returns (RegisterResponse);
}
```

**`course.proto`:**
```protobuf
service CourseService {
  rpc GetCourses    (EmptyCourseRequest) returns (CourseList);
  rpc EnrollStudent (EnrollRequest)      returns (EnrollResponse);
}
```

**`library.proto`:**
```protobuf
service LibraryService {
  rpc SearchBooks (SearchRequest) returns (BookList);
  rpc BorrowBook  (BorrowRequest) returns (BorrowResponse);
}
```

### Microservicios

#### StudentService (puerto 50070)

Gestiona el registro y consulta de estudiantes. Tiene 3 estudiantes precargados y permite registrar nuevos generando IDs automáticos (`EST-00N`).

| Estudiante | Carrera |
|-----------|---------|
| EST-001 — Ana Gomez | Ingeniería de Sistemas |
| EST-002 — Carlos Perez | Ingeniería Civil |
| EST-003 — Laura Torres | Matemáticas |

#### CourseService (puerto 50071)

Catálogo de cursos de la ECI con control de cupos. Valida que el estudiante no se inscriba dos veces al mismo curso y descuenta cupos disponibles en cada matrícula exitosa.

| Código | Nombre | Profesor | Cupos |
|--------|--------|----------|-------|
| ARSW | Arquitecturas de Software | Prof. Martinez | 25 |
| CVDS | Ciclos de Vida del Desarrollo | Prof. Rodriguez | 30 |
| IETI | Intro a Tecnologías de Internet | Prof. Lopez | 20 |

#### LibraryService (puerto 50072)

Búsqueda de libros técnicos por título o autor y préstamo por 14 días. Al prestar un libro lo marca como no disponible. La búsqueda es insensible a mayúsculas.

| ID | Título | Autor |
|----|--------|-------|
| LIB-001 | Clean Code | Robert C. Martin |
| LIB-002 | Designing Data-Intensive Applications | Martin Kleppmann |
| LIB-003 | Microservices Patterns | Chris Richardson |
| LIB-004 | Domain-Driven Design | Eric Evans |
| LIB-005 | The Pragmatic Programmer | Hunt & Thomas |

#### ECICIENCIAGateway (puerto 8095)

Fachada HTTP que enruta cada petición al microservicio gRPC correspondiente. Transforma las respuestas a JSON. Devuelve `404` si un recurso no existe, `409` si hay conflicto (libro no disponible, cupo lleno, ya inscrito) y `400` si faltan parámetros.

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `eciciencia/StudentGrpcServer.java` | **50070** | Estudiantes |
| `eciciencia/CourseGrpcServer.java` | **50071** | Cursos y matrículas |
| `eciciencia/LibraryGrpcServer.java` | **50072** | Libros y préstamos |
| `eciciencia/ECICIENCIAGateway.java` | **8095** | API Gateway HTTP |

### Ejecutar (4 terminales)

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.StudentGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.CourseGrpcServer"

# Terminal 3
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.LibraryGrpcServer"

# Terminal 4 — iniciar después de los 3 servicios
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.ECICIENCIAGateway"
```

### Probar la plataforma

```bash
# Consultar estudiante
curl "http://localhost:8095/api/students?id=EST-001"

# Registrar nuevo estudiante
curl -X POST "http://localhost:8095/api/students/register?name=Juan%20Diaz&email=juan@eci.edu.co&major=Matematicas"

# Ver catálogo de cursos
curl http://localhost:8095/api/courses

# Inscribir estudiante a un curso
curl -X POST "http://localhost:8095/api/courses/enroll?studentId=EST-001&courseId=ARSW"

# Buscar libro por título o autor
curl "http://localhost:8095/api/library/search?query=clean"

# Prestar un libro
curl -X POST "http://localhost:8095/api/library/borrow?studentId=EST-001&bookId=LIB-001"
```

### Reflexión sobre la evolución arquitectónica

Archivo: `src/main/resources/reflexion.html`  
Abre este archivo en un navegador para ver la comparativa visual de todas las arquitecturas, el diagrama de la plataforma ECICIENCIA, los pros/contras del API Gateway y la reflexión final escrita.

---

## Estructura del proyecto

```
src/main/
├── proto/
│   ├── movie.proto              # Parte IV — ejemplo
│   ├── appointment.proto        # Parte IV — ejercicio
│   ├── review.proto             # Parte V — ejemplo
│   ├── recommendation.proto     # Parte V — ejemplo
│   ├── medical.proto            # Parte V — ejercicio
│   ├── gym.proto                # Parte V — ejercicio
│   ├── student.proto            # Ejercicio Final
│   ├── course.proto             # Ejercicio Final
│   └── library.proto            # Ejercicio Final
├── java/edu/eci/arsw/evolucion/
│   ├── movie_tcp/               # Parte I — ejemplo
│   ├── ejercicio_Evolution/     # Parte I — ejercicio
│   ├── MovieHttpServer/         # Parte II — ejemplo
│   ├── ejercicio_http/          # Parte II — ejercicio
│   ├── movie_rmi/               # Parte III — ejemplo
│   ├── ejercicio_rmi/           # Parte III — ejercicio
│   ├── movie_grpc/              # Parte IV — ejemplo
│   ├── ejercicio_grpc/          # Parte IV — ejercicio
│   ├── movie_microservices/     # Parte V — ejemplo
│   ├── ejercicio_microservices/ # Parte V — ejercicio
│   ├── movie_gateway/           # Parte VI — ejemplo
│   ├── wellness_gateway/        # Parte VI — ejercicio
│   └── eciciencia/              # Ejercicio Final
└── resources/
    └── reflexion.html           # Página de reflexión final
```

---

## Referencia rápida de puertos

| Servicio | Puerto | Protocolo | Parte |
|----------|--------|-----------|-------|
| MovieServer | 35000 | TCP | I |
| RoomServer | 31000 | TCP | I |
| MovieHttpServer | 8080 | HTTP | II |
| RoomHttpServer | 8081 | HTTP | II |
| MovieRmiServer | 23000 | RMI/JRMP | III |
| LabRmiServer | 24000 | RMI/JRMP | III |
| MovieGrpcServer | 50051 | gRPC | IV |
| AppointmentGrpcServer | 50052 | gRPC | IV |
| ReviewGrpcServer | 50055 | gRPC | V |
| RecommendationGrpcServer | 50056 | gRPC | V |
| MedicalGrpcServer | 50060 | gRPC | V |
| GymGrpcServer | 50061 | gRPC | V |
| StudentGrpcServer | 50070 | gRPC | Final |
| CourseGrpcServer | 50071 | gRPC | Final |
| LibraryGrpcServer | 50072 | gRPC | Final |
| MovieGateway | 8090 | HTTP | VI |
| WellnessGateway | 8091 | HTTP | VI |
| ECICIENCIAGateway | 8095 | HTTP | Final |
