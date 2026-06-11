package edu.eci.arsw.evolucion.movie_tcp;
public class Movie {
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
    public int getId() {
        return id;
    }
    public String toText() {
        return id + "," + title + "," + director + "," + year;
    }
}

