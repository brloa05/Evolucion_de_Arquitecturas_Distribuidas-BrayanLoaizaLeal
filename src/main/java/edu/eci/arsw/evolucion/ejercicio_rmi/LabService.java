package edu.eci.arsw.evolucion.ejercicio_rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LabService extends Remote {
    List<String> consultarEquipos()              throws RemoteException;
    String       consultarEquipo(String codigo)  throws RemoteException;
    boolean      reservarEquipo(String codigo)   throws RemoteException;
    boolean      liberarEquipo(String codigo)    throws RemoteException;
}
