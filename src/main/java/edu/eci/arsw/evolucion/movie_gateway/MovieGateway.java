package edu.eci.arsw.evolucion.movie_gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.eci.arsw.evolucion.movie_grpc.MovieRequest;
import edu.eci.arsw.evolucion.movie_grpc.MovieResponse;
import edu.eci.arsw.evolucion.movie_grpc.MovieServiceGrpc;
import edu.eci.arsw.evolucion.movie_microservices.RecommendationRequest;
import edu.eci.arsw.evolucion.movie_microservices.RecommendationServiceGrpc;
import edu.eci.arsw.evolucion.movie_microservices.ReviewRequest;
import edu.eci.arsw.evolucion.movie_microservices.ReviewServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * API Gateway para microservicios de peliculas.
 * Expone HTTP en puerto 8090 y agrega internamente:
 *   MovieService       → localhost:50051 (gRPC)
 *   ReviewService      → localhost:50055 (gRPC)
 *   RecommendationService → localhost:50056 (gRPC)
 *
 * Endpoints:
 *   GET /api/movie?id=X  → retorna pelicula + resenias + recomendaciones
 */
public class MovieGateway {

    private final MovieServiceGrpc.MovieServiceBlockingStub movieStub;
    private final ReviewServiceGrpc.ReviewServiceBlockingStub reviewStub;
    private final RecommendationServiceGrpc.RecommendationServiceBlockingStub recStub;

    public MovieGateway() {
        ManagedChannel movieCh  = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        ManagedChannel reviewCh = ManagedChannelBuilder.forAddress("localhost", 50055).usePlaintext().build();
        ManagedChannel recCh    = ManagedChannelBuilder.forAddress("localhost", 50056).usePlaintext().build();

        movieStub  = MovieServiceGrpc.newBlockingStub(movieCh);
        reviewStub = ReviewServiceGrpc.newBlockingStub(reviewCh);
        recStub    = RecommendationServiceGrpc.newBlockingStub(recCh);
    }

    public static void main(String[] args) throws IOException {
        MovieGateway gateway = new MovieGateway();
        HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
        server.createContext("/api/movie", gateway::handleMovie);
        server.start();
        System.out.println("MovieGateway HTTP iniciado en puerto 8090");
        System.out.println("Uso: GET http://localhost:8090/api/movie?id=1");
    }

    private void handleMovie(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);

        if (!params.containsKey("id")) {
            respond(exchange, 400, "{\"error\":\"Parametro 'id' requerido\"}");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(params.get("id"));
        } catch (NumberFormatException e) {
            respond(exchange, 400, "{\"error\":\"'id' debe ser un numero\"}");
            return;
        }

        MovieResponse movie = movieStub.getMovie(MovieRequest.newBuilder().setId(id).build());
        if (!movie.getFound()) {
            respond(exchange, 404, "{\"error\":\"Pelicula no encontrada\"}");
            return;
        }

        StringBuilder reviews = new StringBuilder();
        reviewStub.getReviews(ReviewRequest.newBuilder().setMovieId(id).build())
                .getReviewsList()
                .forEach(r -> reviews.append(String.format(
                        "{\"author\":\"%s\",\"comment\":\"%s\",\"rating\":%d},",
                        r.getAuthor(), r.getComment(), r.getRating())));

        StringBuilder recs = new StringBuilder();
        recStub.getRecommendations(RecommendationRequest.newBuilder().setMovieId(id).build())
                .getTitlesList()
                .forEach(t -> recs.append("\"").append(t).append("\","));

        String reviewsJson = reviews.length() > 0
                ? "[" + reviews.substring(0, reviews.length() - 1) + "]" : "[]";
        String recsJson = recs.length() > 0
                ? "[" + recs.substring(0, recs.length() - 1) + "]" : "[]";

        String json = String.format(
                "{\"id\":%d,\"title\":\"%s\",\"director\":\"%s\",\"year\":%d," +
                "\"reviews\":%s,\"recommendations\":%s}",
                movie.getId(), movie.getTitle(), movie.getDirector(), movie.getYear(),
                reviewsJson, recsJson);

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
