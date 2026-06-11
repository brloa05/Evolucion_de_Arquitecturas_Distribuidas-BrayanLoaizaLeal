package edu.eci.arsw.evolucion.ejercicio_http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.eci.arsw.evolucion.ejercicio_Evolution.Room;
import edu.eci.arsw.evolucion.ejercicio_Evolution.RoomRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class RoomHttpServer {

    public static void main(String[] args) throws Exception {
        RoomRepository repository = new RoomRepository();
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/rooms/reserve", new ReserveHandler(repository));
        server.createContext("/rooms/release", new ReleaseHandler(repository));
        server.createContext("/rooms",          new RoomsHandler(repository));

        server.setExecutor(null);
        server.start();
        System.out.println("RoomHttpServer escuchando en http://localhost:8081");
        System.out.println("  GET  /rooms                -> lista todos los salones");
        System.out.println("  GET  /rooms?id=E303        -> estado de un salon");
        System.out.println("  POST /rooms/reserve?id=E303 -> reservar salon");
        System.out.println("  POST /rooms/release?id=E303 -> liberar salon");
    }


    static class RoomsHandler implements HttpHandler {
        private final RoomRepository repository;

        RoomsHandler(RoomRepository repository) { this.repository = repository; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "<h2>405 - Método no permitido</h2>");
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String body;

            if (query != null && query.startsWith("id=")) {
                String id = query.substring(3);
                Room room = repository.findById(id);
                if (room == null) {
                    body = "<h2>ERROR_SALON_NO_EXISTE: " + id + "</h2>";
                } else {
                    String estado = room.getReservado() ? "RESERVADO" : "DISPONIBLE";
                    body = "<h2>Salón " + room.getId() + " — " + estado + "</h2>";
                }
            } else {
                List<Room> rooms = repository.getAll();
                StringBuilder sb = new StringBuilder("<h2>Salones</h2><ul>");
                for (Room r : rooms) {
                    String estado = r.getReservado() ? "RESERVADO" : "DISPONIBLE";
                    sb.append("<li>").append(r.getId()).append(" — ").append(estado).append("</li>");
                }
                sb.append("</ul>");
                body = sb.toString();
            }

            send(exchange, 200, wrap(body));
        }
    }


    static class ReserveHandler implements HttpHandler {
        private final RoomRepository repository;

        ReserveHandler(RoomRepository repository) { this.repository = repository; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "<h2>405 - Método no permitido</h2>");
                return;
            }
            String id = extractId(exchange.getRequestURI().getQuery());
            if (id == null) {
                send(exchange, 400, wrap("<h2>ERROR: falta parámetro id</h2>"));
                return;
            }
            Room room = repository.findById(id);
            String body;
            if (room == null) {
                body = "<h2>ERROR_SALON_NO_EXISTE: " + id + "</h2>";
            } else if (room.getReservado()) {
                body = "<h2>SALON_RESERVADO — " + id + " ya está reservado</h2>";
            } else {
                room.setReservado(true);
                body = "<h2>RESERVA_EXITOSA — Salón " + id + " reservado correctamente</h2>";
            }
            send(exchange, 200, wrap(body));
        }
    }


    static class ReleaseHandler implements HttpHandler {
        private final RoomRepository repository;

        ReleaseHandler(RoomRepository repository) { this.repository = repository; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "<h2>405 - Método no permitido</h2>");
                return;
            }
            String id = extractId(exchange.getRequestURI().getQuery());
            if (id == null) {
                send(exchange, 400, wrap("<h2>ERROR: falta parámetro id</h2>"));
                return;
            }
            Room room = repository.findById(id);
            String body;
            if (room == null) {
                body = "<h2>ERROR_SALON_NO_EXISTE: " + id + "</h2>";
            } else if (!room.getReservado()) {
                body = "<h2>SALON_DISPONIBLE — " + id + " ya está disponible</h2>";
            } else {
                room.setReservado(false);
                body = "<h2>LIBERACION_EXITOSA — Salón " + id + " liberado correctamente</h2>";
            }
            send(exchange, 200, wrap(body));
        }
    }

    private static String extractId(String query) {
        if (query == null || !query.startsWith("id=")) return null;
        return query.substring(3);
    }

    private static String wrap(String body) {
        return "<html><body>" + body + "</body></html>";
    }

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
