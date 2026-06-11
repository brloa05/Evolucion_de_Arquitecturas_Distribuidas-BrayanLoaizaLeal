package edu.eci.arsw.evolucion.MovieHttpServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.eci.arsw.evolucion.movie_tcp.Movie;
import edu.eci.arsw.evolucion.movie_tcp.MovieRepository;

import java.io.OutputStream;
import java.net.InetSocketAddress;
public class MovieHttpServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        MovieRepository repository = new MovieRepository();
        server.createContext("/movie", new MovieHandler(repository));
        server.setExecutor(null);
        server.start();
        System.out.println("MovieHttpServer escuchando en http://localhost:8080/movie?id=1");
    }
    static class MovieHandler implements HttpHandler {
        private MovieRepository repository;
        public MovieHandler(MovieRepository repository) {
            this.repository = repository;
        }
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                int id = extractId(query);
                Movie movie = repository.findById(id);
                String response;
                if (movie == null) {
                    response = "<html><body><h1>Película no encontrada</h1></body></html>";
                } else {
                    response = "<html><body><h1>" + movie.toText() + "</h1></body></html>";
                }
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int extractId(String query) {
            if (query == null || !query.startsWith("id=")) {
                return -1;
            }
            return Integer.parseInt(query.substring(3));
        }
    }
}