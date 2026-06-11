package edu.eci.arsw.evolucion.movie_rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MovieRmiServer {
    public static void main(String[] args) throws Exception {
        MovieService service = new MovieServiceImpl();
        Registry registry = LocateRegistry.createRegistry(23000);
        registry.rebind("movieService", service);
        System.out.println("MovieService RMI publicado en puerto 23000...");
    }
}
