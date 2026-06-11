package edu.eci.arsw.evolucion;

import edu.eci.arsw.evolucion.movie_tcp.Movie;

/**
 * Punto de entrada de la aplicación.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Evolucion de Arquitecturas Distribuidas");
        Movie movie = new Movie(1, "1984", "Michael Radford", 1984);
        System.out.println(movie.toText());
    }
}
