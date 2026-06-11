package edu.eci.arsw.evolucion.movie_rmi;

import java.io.Serializable;

public class Movie implements Serializable {
    private int id;
    private String title;
    private String director;
    private int year;

    public Movie(int id, String title, String director, int year) {
        this.id = id;
        this.title = title;
        this.director = director;
        this.year = year;
    }

    @Override
    public String toString() {
        return id + " - " + title + " (" + year + ") - " + director;
    }
}
