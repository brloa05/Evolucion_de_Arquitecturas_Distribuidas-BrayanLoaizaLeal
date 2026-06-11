package edu.eci.arsw.evolucion.ejercicio_rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class LabRmiClient {
    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", 24000);
        LabService service = (LabService) registry.lookup("labService");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Inventario de Laboratorios ---");
            System.out.println("1. Consultar todos los equipos");
            System.out.println("2. Consultar equipo por codigo");
            System.out.println("3. Reservar equipo");
            System.out.println("4. Liberar equipo");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion == 0) break;

            switch (opcion) {
                case 1:
                    List<String> equipos = service.consultarEquipos();
                    equipos.forEach(e -> System.out.println("  " + e));
                    break;
                case 2:
                    System.out.print("Codigo del equipo: ");
                    System.out.println(service.consultarEquipo(scanner.nextLine()));
                    break;
                case 3:
                    System.out.print("Codigo del equipo: ");
                    boolean reservado = service.reservarEquipo(scanner.nextLine());
                    System.out.println(reservado ? "RESERVA_EXITOSA" : "ERROR: equipo no disponible o no existe");
                    break;
                case 4:
                    System.out.print("Codigo del equipo: ");
                    boolean liberado = service.liberarEquipo(scanner.nextLine());
                    System.out.println(liberado ? "LIBERACION_EXITOSA" : "ERROR: equipo ya disponible o no existe");
                    break;
                default:
                    System.out.println("Opcion invalida");
            }
        }
        scanner.close();
    }
}
