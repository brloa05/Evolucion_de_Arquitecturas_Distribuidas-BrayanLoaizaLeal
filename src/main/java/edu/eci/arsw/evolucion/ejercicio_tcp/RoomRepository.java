package edu.eci.arsw.evolucion.ejercicio_tcp;

import java.util.HashMap;
import java.util.Map;

public class RoomRepository {
    Map<Integer, Room> repository = new HashMap<>();

    public RoomRepository() {
        repository.put(1, new Room("E301", false));
        repository.put(2, new Room("E302", false));
        repository.put(3, new Room("E303", false));
        repository.put(4, new Room("E304", false));
    }

    public Room findById(String id) {
        for (Room room : repository.values()) {
            if (room.getId().equals(id)) {
                return room;
            }
        }
        return null;
    }

}
