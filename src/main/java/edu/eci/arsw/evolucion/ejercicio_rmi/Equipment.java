package edu.eci.arsw.evolucion.ejercicio_rmi;

import java.io.Serializable;

public class Equipment implements Serializable {
    private String code;
    private String name;
    private String laboratory;
    private boolean available;

    public Equipment(String code, String name, String laboratory) {
        this.code = code;
        this.name = name;
        this.laboratory = laboratory;
        this.available = true;
    }

    public String getCode()        { return code; }
    public String getName()        { return name; }
    public String getLaboratory()  { return laboratory; }
    public boolean isAvailable()   { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return code + " | " + name + " | " + laboratory + " | " + (available ? "DISPONIBLE" : "RESERVADO");
    }
}
