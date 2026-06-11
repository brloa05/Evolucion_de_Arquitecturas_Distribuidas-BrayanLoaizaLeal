package edu.eci.arsw.evolucion.ejercicio_grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class AppointmentGrpcClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        AppointmentServiceGrpc.AppointmentServiceBlockingStub stub =
                AppointmentServiceGrpc.newBlockingStub(channel);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Bienestar Universitario gRPC ---");
            System.out.println("1. Solicitar cita");
            System.out.println("2. Cancelar cita");
            System.out.println("3. Ver mis citas activas");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion == 0) break;

            switch (opcion) {
                case 1:
                    System.out.print("ID estudiante: ");
                    String studentId = scanner.nextLine();
                    System.out.print("Nombre: ");
                    String name = scanner.nextLine();
                    System.out.print("Email institucional: ");
                    String email = scanner.nextLine();
                    System.out.println("Tipo de servicio (0=MEDICINE, 1=PSYCHOLOGY, 2=DENTISTRY): ");
                    int type = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Fecha (ej: 2026-06-20): ");
                    String date = scanner.nextLine();

                    AppointmentResponse res = stub.requestAppointment(
                            AppointmentRequest.newBuilder()
                                    .setStudentId(studentId).setStudentName(name)
                                    .setInstitutionalEmail(email)
                                    .setServiceType(ServiceType.forNumber(type))
                                    .setDate(date).build());
                    System.out.println(res.getMessage() + " | ID cita: " + res.getAppointmentId());
                    break;

                case 2:
                    System.out.print("ID de la cita: ");
                    CancelResponse cancelRes = stub.cancelAppointment(
                            CancelRequest.newBuilder().setAppointmentId(scanner.nextLine()).build());
                    System.out.println(cancelRes.getMessage());
                    break;

                case 3:
                    System.out.print("ID estudiante: ");
                    AppointmentList list = stub.getAppointments(
                            StudentRequest.newBuilder().setStudentId(scanner.nextLine()).build());
                    if (list.getAppointmentsList().isEmpty()) {
                        System.out.println("No hay citas activas");
                    } else {
                        list.getAppointmentsList().forEach(a ->
                                System.out.println("  " + a.getId()
                                        + " | " + a.getServiceType()
                                        + " | " + a.getDate()
                                        + " | " + a.getStatus()));
                    }
                    break;

                default:
                    System.out.println("Opcion invalida");
            }
        }

        channel.shutdown();
        scanner.close();
    }
}
