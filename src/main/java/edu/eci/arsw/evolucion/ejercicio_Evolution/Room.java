package edu.eci.arsw.evolucion.ejercicio_Evolution;

public class Room {
    String id ;
    boolean reservado ;

    public Room(String id, boolean reservado){
        this.id = id;
        this.reservado = reservado;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setReservado(boolean reservado) {
        this.reservado = reservado;
    }

    public String getId() {
        return id;
    }

    public boolean getReservado() {
        return reservado;
    }
}
