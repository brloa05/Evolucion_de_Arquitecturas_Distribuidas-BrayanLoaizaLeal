package edu.eci.arsw.evolucion.wellness_gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentRequest;
import edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentServiceGrpc;
import edu.eci.arsw.evolucion.ejercicio_grpc.ServiceType;
import edu.eci.arsw.evolucion.ejercicio_microservices.EmptyGymRequest;
import edu.eci.arsw.evolucion.ejercicio_microservices.EmptyRequest;
import edu.eci.arsw.evolucion.ejercicio_microservices.GymServiceGrpc;
import edu.eci.arsw.evolucion.ejercicio_microservices.MedicalServiceGrpc;
import edu.eci.arsw.evolucion.ejercicio_microservices.SlotRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Gateway para microservicios de bienestar universitario.
 * Expone HTTP en puerto 8091 y agrega internamente:
 *   AppointmentService → localhost:50052 (gRPC)
 *   MedicalService     → localhost:50060 (gRPC)
 *   GymService         → localhost:50061 (gRPC)
 *
 * Endpoints:
 *   GET  /api/wellness/specialties             → lista especialidades medicas
 *   GET  /api/wellness/gym/slots               → franjas disponibles del gimnasio
 *   POST /api/wellness/gym/reserve             → reservar franja (params: studentId, slot)
 *   POST /api/wellness/appointment             → solicitar cita (params: studentId, name, email, type, date)
 */
public class WellnessGateway {

    private final AppointmentServiceGrpc.AppointmentServiceBlockingStub appointmentStub;
    private final MedicalServiceGrpc.MedicalServiceBlockingStub medicalStub;
    private final GymServiceGrpc.GymServiceBlockingStub gymStub;

    public WellnessGateway() {
        ManagedChannel apptCh    = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
        ManagedChannel medicalCh = ManagedChannelBuilder.forAddress("localhost", 50060).usePlaintext().build();
        ManagedChannel gymCh     = ManagedChannelBuilder.forAddress("localhost", 50061).usePlaintext().build();

        appointmentStub = AppointmentServiceGrpc.newBlockingStub(apptCh);
        medicalStub     = MedicalServiceGrpc.newBlockingStub(medicalCh);
        gymStub         = GymServiceGrpc.newBlockingStub(gymCh);
    }

    public static void main(String[] args) throws IOException {
        WellnessGateway gateway = new WellnessGateway();
        HttpServer server = HttpServer.create(new InetSocketAddress(8091), 0);
        server.createContext("/api/wellness/specialties",   gateway::handleSpecialties);
        server.createContext("/api/wellness/gym/slots",     gateway::handleGymSlots);
        server.createContext("/api/wellness/gym/reserve",   gateway::handleGymReserve);
        server.createContext("/api/wellness/appointment",   gateway::handleAppointment);
        server.start();
        System.out.println("WellnessGateway HTTP iniciado en puerto 8091");
        System.out.println("Endpoints disponibles:");
        System.out.println("  GET  http://localhost:8091/api/wellness/specialties");
        System.out.println("  GET  http://localhost:8091/api/wellness/gym/slots");
        System.out.println("  POST http://localhost:8091/api/wellness/gym/reserve?studentId=X&slot=Y");
        System.out.println("  POST http://localhost:8091/api/wellness/appointment?studentId=X&name=Y&email=Z&type=0&date=2026-06-20");
    }

    private void handleSpecialties(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

        StringBuilder sb = new StringBuilder("[");
        medicalStub.getSpecialties(EmptyRequest.newBuilder().build())
                .getSpecialtiesList()
                .forEach(s -> sb.append(String.format(
                        "{\"name\":\"%s\",\"availableSlots\":%d,\"schedule\":\"%s\"},",
                        s.getName(), s.getAvailableSlots(), s.getSchedule())));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("]");
        respond(exchange, 200, sb.toString());
    }

    private void handleGymSlots(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

        StringBuilder sb = new StringBuilder("[");
        gymStub.getAvailableSlots(EmptyGymRequest.newBuilder().build())
                .getSlotsList()
                .forEach(s -> sb.append("\"").append(s).append("\","));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("]");
        respond(exchange, 200, sb.toString());
    }

    private void handleGymReserve(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String studentId = params.get("studentId");
        String slot      = params.getOrDefault("slot", "").replace("+", " ");

        if (studentId == null || slot.isEmpty()) {
            respond(exchange, 400, "{\"error\":\"Parametros requeridos: studentId, slot\"}");
            return;
        }

        var res = gymStub.reserveSlot(SlotRequest.newBuilder().setStudentId(studentId).setTimeSlot(slot).build());
        String json = String.format("{\"success\":%b,\"message\":\"%s\"}", res.getSuccess(), res.getMessage());
        respond(exchange, res.getSuccess() ? 200 : 409, json);
    }

    private void handleAppointment(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { respond(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String studentId = params.get("studentId");
        String name      = params.get("name");
        String email     = params.get("email");
        String typeStr   = params.get("type");
        String date      = params.get("date");

        if (studentId == null || name == null || email == null || typeStr == null || date == null) {
            respond(exchange, 400, "{\"error\":\"Parametros requeridos: studentId, name, email, type, date\"}");
            return;
        }

        int typeNum;
        try { typeNum = Integer.parseInt(typeStr); } catch (NumberFormatException e) {
            respond(exchange, 400, "{\"error\":\"'type' debe ser 0, 1 o 2\"}"); return;
        }

        var res = appointmentStub.requestAppointment(AppointmentRequest.newBuilder()
                .setStudentId(studentId).setStudentName(name).setInstitutionalEmail(email)
                .setServiceType(ServiceType.forNumber(typeNum)).setDate(date).build());

        String json = String.format("{\"appointmentId\":\"%s\",\"message\":\"%s\",\"status\":\"%s\"}",
                res.getAppointmentId(), res.getMessage(), res.getStatus());
        respond(exchange, 200, json);
    }

    private void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .filter(p -> p.length == 2)
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));
    }
}
