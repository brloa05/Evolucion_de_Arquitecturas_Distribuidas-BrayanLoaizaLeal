# Taller Integrador ARSW 2026-I
## Evolución de Arquitecturas Distribuidas con Java

**Autor:** Brayan Loaiza Leal  
**Curso:** Arquitecturas de Software (ARSW) — Escuela Colombiana de Ingeniería Julio Garavito  
**Periodo:** 2026-I

---

## Descripción

Este proyecto implementa el mismo dominio (películas/salas y bienestar universitario) a través de **6 estilos arquitectónicos** progresivos, mostrando cómo evolucionan las arquitecturas distribuidas en Java:

| Parte | Arquitectura | Tecnología | Puerto(s) |
|-------|-------------|------------|-----------|
| I | TCP Sockets | `ServerSocket` / `Socket` | 35000 / 31000 |
| II | HTTP | `com.sun.net.httpserver` | 8080 / 8081 |
| III | RMI | `java.rmi` | 23000 / 24000 |
| IV | gRPC | Protocol Buffers + HTTP/2 | 50051 / 50052 |
| V | Microservicios | gRPC (múltiples servicios) | 50055 / 50056 / 50060 / 50061 |
| VI | API Gateway | HTTP facade sobre gRPC | 8090 / 8091 / 8095 |

---

## Requisitos

- Java 17+
- Maven 3.8+
- `protoc` (gestionado automáticamente por `protobuf-maven-plugin`)

## Compilar el proyecto

```bash
mvn compile
```

---

## Parte I — TCP Sockets

Comunicación de bajo nivel mediante sockets. El servidor acepta conexiones, lee texto plano y responde sin ningún protocolo estándar.

### Ejemplo: Movie TCP Server

| Clase | Descripción |
|-------|-------------|
| `movie_tcp/MovieServer.java` | Servidor TCP en puerto **35000** |
| `movie_tcp/MovieClient.java` | Cliente que consulta películas por ID |
| `movie_tcp/Movie.java` | Modelo de película |
| `movie_tcp/MovieRepository.java` | Repositorio en memoria |

**Ejecutar:**
```bash
# Terminal 1 — Servidor
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_tcp.MovieServer"

# Terminal 2 — Cliente
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_tcp.MovieClient"
```

**Protocolo:** `get movie <id>` → respuesta en texto plano

---

### Ejercicio: Room TCP Server

| Clase | Descripción |
|-------|-------------|
| `ejercicio_Evolution/RoomServer.java` | Servidor TCP en puerto **31000** |
| `ejercicio_Evolution/RoomClient.java` | Cliente interactivo |
| `ejercicio_Evolution/Room.java` | Modelo de sala |
| `ejercicio_Evolution/RoomRepository.java` | Repositorio con salas precargadas |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_Evolution.RoomServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_Evolution.RoomClient"
```

**Comandos disponibles:**
```
get rooms              → lista todas las salas
get room <id>          → detalle de una sala
reserve room <id>      → reservar sala
release room <id>      → liberar sala
```

---

## Parte II — HTTP

Protocolo estándar sobre TCP con verbos, rutas y códigos de estado. El cliente puede ser cualquier navegador o herramienta REST.

### Ejemplo: Movie HTTP Server

| Clase | Descripción |
|-------|-------------|
| `MovieHttpServer/MovieHttpServer.java` | Servidor HTTP en puerto **8080** |

**Ejecutar:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.MovieHttpServer.MovieHttpServer"
```

**Endpoint:**
```
GET http://localhost:8080/movie?id=1
```

---

### Ejercicio: Room HTTP Server

| Clase | Descripción |
|-------|-------------|
| `ejercicio_http/RoomHttpServer.java` | Servidor HTTP en puerto **8081** |

**Ejecutar:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_http.RoomHttpServer"
```

**Endpoints:**
```
GET  http://localhost:8081/rooms              → lista todas las salas
GET  http://localhost:8081/rooms?id=1         → detalle de sala
POST http://localhost:8081/rooms/reserve?id=1 → reservar sala
POST http://localhost:8081/rooms/release?id=1 → liberar sala
```

---

## Parte III — RMI (Remote Method Invocation)

Invocación remota de métodos Java. El cliente llama métodos como si fueran locales. Fuertemente tipado pero acoplado a la JVM.

### Ejemplo: Movie RMI

| Clase | Descripción |
|-------|-------------|
| `movie_rmi/MovieRmiServer.java` | Registry + servidor en puerto **23000** |
| `movie_rmi/MovieRmiClient.java` | Cliente RMI |
| `movie_rmi/MovieService.java` | Interfaz remota |
| `movie_rmi/MovieServiceImpl.java` | Implementación |
| `movie_rmi/Movie.java` | Serializable |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_rmi.MovieRmiServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_rmi.MovieRmiClient"
```

---

### Ejercicio: Lab Inventory RMI

Inventario de equipos de laboratorio (PCs, Realidad Aumentada, Sistemas Operativos).

| Clase | Descripción |
|-------|-------------|
| `ejercicio_rmi/LabRmiServer.java` | Registry + servidor en puerto **24000** |
| `ejercicio_rmi/LabRmiClient.java` | Menú interactivo |
| `ejercicio_rmi/LabService.java` | Interfaz remota |
| `ejercicio_rmi/LabServiceImpl.java` | 5 equipos precargados |
| `ejercicio_rmi/Equipment.java` | Modelo serializable |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_rmi.LabRmiServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_rmi.LabRmiClient"
```

**Equipos disponibles:** `PC-001`, `PC-002`, `AR-001`, `AR-002`, `OS-001`

---

## Parte IV — gRPC

Contratos en Protocol Buffers (`.proto`). Alta eficiencia con HTTP/2, tipado fuerte, stubs generados automáticamente.

### Ejemplo: Movie gRPC

**Contrato `movie.proto`:**
```protobuf
service MovieService {
  rpc GetMovie (MovieRequest) returns (MovieResponse);
}
```

| Clase | Descripción |
|-------|-------------|
| `movie_grpc/MovieGrpcServer.java` | Servidor gRPC en puerto **50051** |
| `movie_grpc/MovieGrpcClient.java` | Cliente gRPC |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcClient"
```

---

### Ejercicio: Appointment gRPC (Bienestar)

Sistema de citas médicas universitarias.

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
| `ejercicio_grpc/AppointmentGrpcServer.java` | Servidor gRPC en puerto **50052** |
| `ejercicio_grpc/AppointmentGrpcClient.java` | Menú interactivo |

**Ejecutar:**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcClient"
```

**Tipos de servicio:** `0 = MEDICINE`, `1 = PSYCHOLOGY`, `2 = DENTISTRY`

---

## Parte V — Microservicios

Cada responsabilidad es un servicio gRPC independiente. El cliente agrega múltiples servicios en una sola vista.

### Microservicios de Películas

```
MovieService          → localhost:50051  (reutilizado de Parte IV)
ReviewService         → localhost:50055
RecommendationService → localhost:50056
```

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `movie_microservices/ReviewGrpcServer.java` | **50055** | Reseñas por película |
| `movie_microservices/RecommendationGrpcServer.java` | **50056** | Recomendaciones por película |
| `movie_microservices/MovieMicroClient.java` | — | Cliente que agrega los 3 servicios |

**Ejecutar (4 terminales):**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_grpc.MovieGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.ReviewGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.RecommendationGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_microservices.MovieMicroClient"
```

---

### Microservicios de Bienestar

```
AppointmentService → localhost:50052  (reutilizado de Parte IV)
MedicalService     → localhost:50060
GymService         → localhost:50061
```

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `ejercicio_microservices/MedicalGrpcServer.java` | **50060** | Especialidades médicas con cupos y horarios |
| `ejercicio_microservices/GymGrpcServer.java` | **50061** | Franjas del gimnasio con reserva |
| `ejercicio_microservices/WellnessClient.java` | — | Cliente que agrega los 3 servicios de bienestar |

**Ejecutar (4 terminales):**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.MedicalGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.GymGrpcServer"
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.ejercicio_microservices.WellnessClient"
```

---

## Parte VI — API Gateway

Punto único de entrada HTTP que oculta la complejidad interna de los microservicios. El cliente solo habla con el gateway.

### MovieGateway (puerto 8090)

Agrega: `MovieService (50051)` + `ReviewService (50055)` + `RecommendationService (50056)`

**Iniciar microservicios primero, luego el gateway:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.movie_gateway.MovieGateway"
```

**Endpoint:**
```
GET http://localhost:8090/api/movie?id=1
```

**Respuesta JSON:**
```json
{
  "id": 1,
  "title": "Interstellar",
  "director": "Christopher Nolan",
  "year": 2014,
  "reviews": [
    { "author": "Alice", "comment": "Excelente pelicula", "rating": 5 }
  ],
  "recommendations": ["Inception", "Contact", "2001: A Space Odyssey"]
}
```

---

### WellnessGateway (puerto 8091)

Agrega: `AppointmentService (50052)` + `MedicalService (50060)` + `GymService (50061)`

**Iniciar microservicios primero, luego el gateway:**
```bash
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.wellness_gateway.WellnessGateway"
```

**Endpoints:**
```
GET  http://localhost:8091/api/wellness/specialties
GET  http://localhost:8091/api/wellness/gym/slots
POST http://localhost:8091/api/wellness/gym/reserve?studentId=EST-001&slot=07:00+-+08:00
POST http://localhost:8091/api/wellness/appointment?studentId=EST-001&name=Ana&email=ana@eci.edu.co&type=0&date=2026-06-20
```

---

## Ejercicio Final — Plataforma ECICIENCIA

Plataforma académica integral que aplica la arquitectura de microservicios + API Gateway sobre un nuevo dominio universitario.

### Arquitectura

```
                   ┌──────────────────────────┐
                   │   Cliente (HTTP :8095)   │
                   └────────────┬─────────────┘
                                │
                   ┌────────────▼─────────────┐
                   │    ECICIENCIAGateway     │
                   └───┬──────────┬───────────┘
                       │          │          │
                   :50070      :50071     :50072
              ┌────────┐  ┌─────────┐  ┌──────────┐
              │Student │  │ Course  │  │ Library  │
              │Service │  │ Service │  │ Service  │
              └────────┘  └─────────┘  └──────────┘
```

### Contratos Proto

| Archivo | Servicio | Operaciones |
|---------|----------|-------------|
| `student.proto` | `StudentService` | `GetStudent`, `RegisterStudent` |
| `course.proto` | `CourseService` | `GetCourses`, `EnrollStudent` |
| `library.proto` | `LibraryService` | `SearchBooks`, `BorrowBook` |

### Microservicios

| Clase | Puerto | Descripción |
|-------|--------|-------------|
| `eciciencia/StudentGrpcServer.java` | **50070** | Estudiantes: consulta y registro |
| `eciciencia/CourseGrpcServer.java` | **50071** | Cursos ARSW, CVDS, IETI + matrícula |
| `eciciencia/LibraryGrpcServer.java` | **50072** | 5 libros técnicos + búsqueda y préstamo |
| `eciciencia/ECICIENCIAGateway.java` | **8095** | API Gateway HTTP que agrega los 3 servicios |

**Ejecutar (4 terminales):**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.StudentGrpcServer"

# Terminal 2
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.CourseGrpcServer"

# Terminal 3
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.LibraryGrpcServer"

# Terminal 4 — API Gateway
mvn exec:java -Dexec.mainClass="edu.eci.arsw.evolucion.eciciencia.ECICIENCIAGateway"
```

**Endpoints:**
```
GET  http://localhost:8095/api/students?id=EST-001
POST http://localhost:8095/api/students/register?name=X&email=Y&major=Z
GET  http://localhost:8095/api/courses
POST http://localhost:8095/api/courses/enroll?studentId=EST-001&courseId=ARSW
GET  http://localhost:8095/api/library/search?query=clean
POST http://localhost:8095/api/library/borrow?studentId=EST-001&bookId=LIB-001
```

**Datos precargados:**

| Tipo | IDs disponibles |
|------|----------------|
| Estudiantes | `EST-001` Ana Gomez · `EST-002` Carlos Perez · `EST-003` Laura Torres |
| Cursos | `ARSW` · `CVDS` · `IETI` |
| Libros | `LIB-001` a `LIB-005` (Clean Code, DDIA, Microservices Patterns, DDD, Pragmatic Programmer) |

### Reflexión sobre la evolución arquitectónica

Ver el archivo: `src/main/resources/reflexion.html`

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

## Resumen de puertos

| Servicio | Puerto | Protocolo |
|----------|--------|-----------|
| MovieServer (TCP) | 35000 | TCP |
| RoomServer (TCP) | 31000 | TCP |
| MovieHttpServer | 8080 | HTTP |
| RoomHttpServer | 8081 | HTTP |
| MovieRmiServer | 23000 | RMI/JRMP |
| LabRmiServer | 24000 | RMI/JRMP |
| MovieGrpcServer | 50051 | gRPC |
| AppointmentGrpcServer | 50052 | gRPC |
| ReviewGrpcServer | 50055 | gRPC |
| RecommendationGrpcServer | 50056 | gRPC |
| MedicalGrpcServer | 50060 | gRPC |
| GymGrpcServer | 50061 | gRPC |
| StudentGrpcServer | 50070 | gRPC |
| CourseGrpcServer | 50071 | gRPC |
| LibraryGrpcServer | 50072 | gRPC |
| MovieGateway | 8090 | HTTP |
| WellnessGateway | 8091 | HTTP |
| ECICIENCIAGateway | 8095 | HTTP |
