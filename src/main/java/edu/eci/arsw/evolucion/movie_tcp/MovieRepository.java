package edu.eci.arsw.evolucion.movie_tcp;

import java.util.HashMap;
import java.util.Map;
public class MovieRepository {
    private Map<Integer, Movie> movies = new HashMap<>();
    public MovieRepository() {
        movies.put(1, new Movie(1, "Interstellar", "Christopher Nolan", 2014));
        movies.put(2, new Movie(2, "Matrix", "Wachowski", 1999));
        movies.put(3, new Movie(3, "Inception", "Christopher Nolan", 2010));
    }
    public Movie findById(int id) {
        return movies.get(id);
    }
}
