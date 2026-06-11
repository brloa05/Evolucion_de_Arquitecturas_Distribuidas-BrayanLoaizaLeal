package edu.eci.arsw.evolucion.ejercicio_Evolution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class RoomClient {
    public static void main(String[] args) {
        Socket socket;

        {
            try {
                socket = new Socket("127.0.0.1", 31000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in);

                System.out.println("Consultas basicas ejemplos: \nCONSULTAR_SALON,E303 \nRESERVAR_SALON,E303 \nLIBERAR_SALON,E303 \nEXIT");
                while (true) {
                    String option = scanner.nextLine();
                    if (option.equals("EXIT")) {
                        out.println("EXIT");
                        break;
                    }
                    out.println(option);
                    System.out.println(in.readLine());
                }
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
