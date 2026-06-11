package edu.eci.arsw.evolucion.ejercicio_microservices;

import edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentRequest;
import edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentResponse;
import edu.eci.arsw.evolucion.ejercicio_grpc.AppointmentServiceGrpc;
import edu.eci.arsw.evolucion.ejercicio_grpc.ServiceType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

/**
 * Cliente que agrega los microservicios de bienestar:
 *   AppointmentService → localhost:50052
 *   MedicalService     → localhost:50060
 *   GymService         → localhost:50061
 */
public class WellnessClient {

    public static void main(String[] args) {
        ManagedChannel appointmentChannel = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
        ManagedChannel medicalChannel     = ManagedChannelBuilder.forAddress("localhost", 50060).usePlaintext().build();
        ManagedChannel gymChannel         = ManagedChannelBuilder.forAddress("localhost", 50061).usePlaintext().build();

        AppointmentServiceGrpc.AppointmentServiceBlockingStub appointmentStub =
                AppointmentServiceGrpc.newBlockingStub(appointmentChannel);
        MedicalServiceGrpc.MedicalServiceBlockingStub medicalStub =
                MedicalServiceGrpc.newBlockingStub(medicalChannel);
        GymServiceGrpc.GymServiceBlockingStub gymStub =
                GymServiceGrpc.newBlockingStub(gymChannel);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Portal de Bienestar Universitario ===");
            System.out.println("1. Ver especialidades medicas");
            System.out.println("2. Solicitar cita medica");
            System.out.println("3. Ver franjas de gimnasio disponibles");
            System.out.println("4. Reservar franja de gimnasio");
            System.out.println("0. Salir");
            System.out.print("Opcion: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion == 0) break;

            switch (opcion) {
                case 1:
                    medicalStub.getSpecialties(EmptyRequest.newBuilder().build())
                            .getSpecialtiesList()
                            .forEach(s -> System.out.println(
                                    "  " + s.getName() + " | Cupos: " + s.getAvailableSlots() + " | " + s.getSchedule()));
                    break;
                case 2:
                    System.out.print("ID estudiante: ");   String sid   = scanner.nextLine();
                    System.out.print("Nombre: ");          String sname = scanner.nextLine();
                    System.out.print("Email: ");           String email = scanner.nextLine();
                    System.out.println("Tipo (0=MEDICINE, 1=PSYCHOLOGY, 2=DENTISTRY): ");
                    int type = scanner.nextInt(); scanner.nextLine();
                    System.out.print("Fecha (ej: 2026-06-20): "); String date = scanner.nextLine();

                    AppointmentResponse res = appointmentStub.requestAppointment(
                            AppointmentRequest.newBuilder()
                                    .setStudentId(sid).setStudentName(sname).setInstitutionalEmail(email)
                                    .setServiceType(ServiceType.forNumber(type)).setDate(date).build());
                    System.out.println(res.getMessage() + " | ID: " + res.getAppointmentId());
                    break;
                case 3:
                    gymStub.getAvailableSlots(EmptyGymRequest.newBuilder().build())
                            .getSlotsList()
                            .forEach(s -> System.out.println("  " + s));
                    break;
                case 4:
                    System.out.print("ID estudiante: ");           String gsid = scanner.nextLine();
                    System.out.print("Franja (ej: 07:00 - 08:00): "); String slot = scanner.nextLine();
                    SlotResponse gres = gymStub.reserveSlot(
                            SlotRequest.newBuilder().setStudentId(gsid).setTimeSlot(slot).build());
                    System.out.println(gres.getMessage());
                    break;
                default:
                    System.out.println("Opcion invalida");
            }
        }

        appointmentChannel.shutdown();
        medicalChannel.shutdown();
        gymChannel.shutdown();
        scanner.close();
    }
}
