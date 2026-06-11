package edu.eci.arsw.evolucion.eciciencia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Gateway de la plataforma ECICIENCIA.
 * Puerto HTTP: 8095
 *
 * Agrega internamente (gRPC):
 *   StudentService → localhost:50070
 *   CourseService  → localhost:50071
 *   LibraryService → localhost:50072
 *
 * Endpoints expuestos:
 *   GET  /api/students?id=EST-001
 *   POST /api/students/register?name=X&email=Y&major=Z
 *   GET  /api/courses
 *   POST /api/courses/enroll?studentId=X&courseId=Y
 *   GET  /api/library/search?query=X
 *   POST /api/library/borrow?studentId=X&bookId=Y
 */
public class ECICIENCIAGateway {

    private final StudentServiceGrpc.StudentServiceBlockingStub studentStub;
    private final CourseServiceGrpc.CourseServiceBlockingStub   courseStub;
    private final LibraryServiceGrpc.LibraryServiceBlockingStub libraryStub;

    public ECICIENCIAGateway() {
        ManagedChannel studentCh = ManagedChannelBuilder.forAddress("localhost", 50070).usePlaintext().build();
        ManagedChannel courseCh  = ManagedChannelBuilder.forAddress("localhost", 50071).usePlaintext().build();
        ManagedChannel libraryCh = ManagedChannelBuilder.forAddress("localhost", 50072).usePlaintext().build();

        studentStub = StudentServiceGrpc.newBlockingStub(studentCh);
        courseStub  = CourseServiceGrpc.newBlockingStub(courseCh);
        libraryStub = LibraryServiceGrpc.newBlockingStub(libraryCh);
    }

    public static void main(String[] args) throws IOException {
        ECICIENCIAGateway gw = new ECICIENCIAGateway();
        HttpServer server = HttpServer.create(new InetSocketAddress(8095), 0);

        server.createContext("/api/students/register", gw::handleRegisterStudent);
        server.createContext("/api/students",          gw::handleGetStudent);
        server.createContext("/api/courses/enroll",    gw::handleEnroll);
        server.createContext("/api/courses",           gw::handleGetCourses);
        server.createContext("/api/library/borrow",    gw::handleBorrow);
        server.createContext("/api/library/search",    gw::handleSearch);

        server.start();
        System.out.println("ECICIENCIA API Gateway iniciado en puerto 8095");
        System.out.println("Endpoints:");
        System.out.println("  GET  /api/students?id=EST-001");
        System.out.println("  POST /api/students/register?name=X&email=Y&major=Z");
        System.out.println("  GET  /api/courses");
        System.out.println("  POST /api/courses/enroll?studentId=X&courseId=Y");
        System.out.println("  GET  /api/library/search?query=X");
        System.out.println("  POST /api/library/borrow?studentId=X&bookId=Y");
    }

    private void handleGetStudent(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        Map<String, String> p = params(ex);
        String id = p.get("id");
        if (id == null) { respond(ex, 400, err("Parametro 'id' requerido")); return; }

        StudentResponse s = studentStub.getStudent(GetStudentRequest.newBuilder().setStudentId(id).build());
        if (!s.getFound()) { respond(ex, 404, err("Estudiante no encontrado")); return; }
        respond(ex, 200, String.format(
                "{\"studentId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"major\":\"%s\"}",
                s.getStudentId(), s.getName(), s.getEmail(), s.getMajor()));
    }

    private void handleRegisterStudent(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        Map<String, String> p = params(ex);
        if (!p.containsKey("name") || !p.containsKey("email") || !p.containsKey("major")) {
            respond(ex, 400, err("Parametros requeridos: name, email, major")); return;
        }
        RegisterResponse r = studentStub.registerStudent(RegisterRequest.newBuilder()
                .setName(p.get("name")).setEmail(p.get("email")).setMajor(p.get("major")).build());
        respond(ex, 201, String.format("{\"studentId\":\"%s\",\"message\":\"%s\",\"success\":%b}",
                r.getStudentId(), r.getMessage(), r.getSuccess()));
    }

    private void handleGetCourses(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        StringBuilder sb = new StringBuilder("[");
        courseStub.getCourses(EmptyCourseRequest.newBuilder().build()).getCoursesList().forEach(c ->
                sb.append(String.format("{\"courseId\":\"%s\",\"name\":\"%s\",\"professor\":\"%s\",\"availableSlots\":%d},",
                        c.getCourseId(), c.getName(), c.getProfessor(), c.getAvailableSlots())));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("]");
        respond(ex, 200, sb.toString());
    }

    private void handleEnroll(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        Map<String, String> p = params(ex);
        if (!p.containsKey("studentId") || !p.containsKey("courseId")) {
            respond(ex, 400, err("Parametros requeridos: studentId, courseId")); return;
        }
        EnrollResponse r = courseStub.enrollStudent(EnrollRequest.newBuilder()
                .setStudentId(p.get("studentId")).setCourseId(p.get("courseId")).build());
        respond(ex, r.getSuccess() ? 200 : 409,
                String.format("{\"success\":%b,\"message\":\"%s\"}", r.getSuccess(), r.getMessage()));
    }

    private void handleSearch(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        String query = params(ex).getOrDefault("query", "");
        StringBuilder sb = new StringBuilder("[");
        libraryStub.searchBooks(SearchRequest.newBuilder().setQuery(query).build()).getBooksList().forEach(b ->
                sb.append(String.format("{\"bookId\":\"%s\",\"title\":\"%s\",\"author\":\"%s\",\"available\":%b},",
                        b.getBookId(), b.getTitle(), b.getAuthor(), b.getAvailable())));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("]");
        respond(ex, 200, sb.toString());
    }

    private void handleBorrow(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, err("Method not allowed")); return; }
        Map<String, String> p = params(ex);
        if (!p.containsKey("studentId") || !p.containsKey("bookId")) {
            respond(ex, 400, err("Parametros requeridos: studentId, bookId")); return;
        }
        BorrowResponse r = libraryStub.borrowBook(BorrowRequest.newBuilder()
                .setStudentId(p.get("studentId")).setBookId(p.get("bookId")).build());
        respond(ex, r.getSuccess() ? 200 : 409,
                String.format("{\"success\":%b,\"message\":\"%s\",\"dueDate\":\"%s\"}",
                        r.getSuccess(), r.getMessage(), r.getDueDate()));
    }

    private void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private Map<String, String> params(HttpExchange ex) {
        String q = ex.getRequestURI().getQuery();
        if (q == null || q.isEmpty()) return Map.of();
        return Arrays.stream(q.split("&")).map(s -> s.split("=", 2))
                .filter(s -> s.length == 2).collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }

    private String err(String msg) {
        return "{\"error\":\"" + msg + "\"}";
    }
}
