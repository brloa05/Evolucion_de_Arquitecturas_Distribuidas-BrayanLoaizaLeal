package edu.eci.arsw.evolucion.ejercicio_grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50052)
                .addService(new AppointmentServiceImpl())
                .build();
        server.start();
        System.out.println("AppointmentService gRPC Server iniciado en puerto 50052");
        server.awaitTermination();
    }

    static class AppointmentServiceImpl extends AppointmentServiceGrpc.AppointmentServiceImplBase {
        private final Map<String, Appointment> appointments = new HashMap<>();
        private int counter = 1;

        @Override
        public void requestAppointment(AppointmentRequest request,
                                       StreamObserver<AppointmentResponse> responseObserver) {
            String id = "APT-" + counter++;
            Appointment appointment = Appointment.newBuilder()
                    .setId(id)
                    .setStudentId(request.getStudentId())
                    .setServiceType(request.getServiceType())
                    .setDate(request.getDate())
                    .setStatus(AppointmentStatus.REQUESTED)
                    .build();
            appointments.put(id, appointment);

            responseObserver.onNext(AppointmentResponse.newBuilder()
                    .setAppointmentId(id)
                    .setSuccess(true)
                    .setMessage("Cita registrada en estado REQUESTED")
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void cancelAppointment(CancelRequest request,
                                      StreamObserver<CancelResponse> responseObserver) {
            Appointment apt = appointments.get(request.getAppointmentId());
            CancelResponse response;
            if (apt == null || apt.getStatus() == AppointmentStatus.CANCELLED) {
                response = CancelResponse.newBuilder()
                        .setSuccess(false).setMessage("Cita no encontrada o ya cancelada").build();
            } else {
                appointments.put(apt.getId(),
                        apt.toBuilder().setStatus(AppointmentStatus.CANCELLED).build());
                response = CancelResponse.newBuilder()
                        .setSuccess(true).setMessage("Cita cancelada exitosamente").build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getAppointments(StudentRequest request,
                                    StreamObserver<AppointmentList> responseObserver) {
            List<Appointment> result = new ArrayList<>();
            for (Appointment apt : appointments.values()) {
                if (apt.getStudentId().equals(request.getStudentId())
                        && apt.getStatus() != AppointmentStatus.CANCELLED) {
                    result.add(apt);
                }
            }
            responseObserver.onNext(AppointmentList.newBuilder().addAllAppointments(result).build());
            responseObserver.onCompleted();
        }
    }
}
