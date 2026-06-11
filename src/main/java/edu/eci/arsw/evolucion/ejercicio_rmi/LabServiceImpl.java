package edu.eci.arsw.evolucion.ejercicio_rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabServiceImpl extends UnicastRemoteObject implements LabService {
    private final Map<String, Equipment> equipments = new HashMap<>();

    public LabServiceImpl() throws RemoteException {
        equipments.put("PC-001", new Equipment("PC-001", "Computador Dell",  "Lab Redes"));
        equipments.put("PC-002", new Equipment("PC-002", "Computador HP",    "Lab Redes"));
        equipments.put("AR-001", new Equipment("AR-001", "Arduino Uno",      "Lab Electronica"));
        equipments.put("AR-002", new Equipment("AR-002", "Arduino Mega",     "Lab Electronica"));
        equipments.put("OS-001", new Equipment("OS-001", "Osciloscopio",     "Lab Senales"));
    }

    @Override
    public List<String> consultarEquipos() throws RemoteException {
        List<String> result = new ArrayList<>();
        for (Equipment e : equipments.values()) result.add(e.toString());
        return result;
    }

    @Override
    public String consultarEquipo(String codigo) throws RemoteException {
        Equipment e = equipments.get(codigo);
        return e == null ? "ERROR: equipo no encontrado" : e.toString();
    }

    @Override
    public boolean reservarEquipo(String codigo) throws RemoteException {
        Equipment e = equipments.get(codigo);
        if (e == null || !e.isAvailable()) return false;
        e.setAvailable(false);
        return true;
    }

    @Override
    public boolean liberarEquipo(String codigo) throws RemoteException {
        Equipment e = equipments.get(codigo);
        if (e == null || e.isAvailable()) return false;
        e.setAvailable(true);
        return true;
    }
}
