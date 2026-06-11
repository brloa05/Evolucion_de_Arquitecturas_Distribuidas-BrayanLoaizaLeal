package edu.eci.arsw.evolucion.ejercicio_tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class RoomServer {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(31000);
            System.out.println("RoomServer is listening to port 31000...");
            RoomRepository repositoryN = new RoomRepository();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("EXIT")) break;
                    String[] parts = line.split(",");
                    if (parts.length < 2) {
                        out.println("ERROR_FORMATO_INVALIDO. Use: OPERACION,ID_SALON");
                        continue;
                    }
                    out.println(queries(parts[0], parts[1], repositoryN));
                }
                in.close();
                out.close();
                clientSocket.close();

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    public static String queries (String petition, String id, RoomRepository repository) {
        Room room = repository.findById(id);
        if (room== null) return "ERROR_SALON_NO_EXISTE";

        if (petition.equals("CONSULTAR_SALON")) {
            if (!room.getReservado()) {
                return "SALON_DISPONIBLE";
            }
            return "SALON_RESERVADO";
        } else if (petition.equals("RESERVAR_SALON")) {
            if (!room.getReservado()) {
                room.setReservado(true);
                return "RESERVA EXITOSA";
            }
            return "SALON_RESERVADO";
        } else if (petition.equals("LIBERAR_SALON")) {
            if (room.getReservado()) {
                room.setReservado(false);
                return "LIBERACION_EXITOSA";
            }
            return "SALON DISPONIBLE";
        } else {
            return "ERROR_OPERACION_INVALIDA";
        }
    }


}
