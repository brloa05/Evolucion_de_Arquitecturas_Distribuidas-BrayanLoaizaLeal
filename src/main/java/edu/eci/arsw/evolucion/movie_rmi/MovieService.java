package edu.eci.arsw.evolucion.movie_rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MovieService extends Remote {
    Movie getMovie(int id) throws RemoteException;
}
