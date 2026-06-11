package edu.eci.arsw.evolucion.ejercicio_microservices;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;

public class MedicalGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50060)
                .addService(new MedicalServiceImpl())
                .build();
        server.start();
        System.out.println("MedicalService gRPC iniciado en puerto 50060");
        server.awaitTermination();
    }

    static class MedicalServiceImpl extends MedicalServiceGrpc.MedicalServiceImplBase {
        private final List<SpecialtyInfo> specialties = Arrays.asList(
                SpecialtyInfo.newBuilder().setName("MEDICINE")
                        .setAvailableSlots(5).setSchedule("Lunes-Viernes 8am-12pm").build(),
                SpecialtyInfo.newBuilder().setName("PSYCHOLOGY")
                        .setAvailableSlots(3).setSchedule("Martes-Jueves 2pm-5pm").build(),
                SpecialtyInfo.newBuilder().setName("DENTISTRY")
                        .setAvailableSlots(2).setSchedule("Miercoles 9am-11am").build()
        );

        @Override
        public void getSpecialties(EmptyRequest request, StreamObserver<SpecialtyList> responseObserver) {
            responseObserver.onNext(SpecialtyList.newBuilder().addAllSpecialties(specialties).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getSpecialtyInfo(SpecialtyRequest request, StreamObserver<SpecialtyInfo> responseObserver) {
            SpecialtyInfo info = specialties.stream()
                    .filter(s -> s.getName().equals(request.getName()))
                    .findFirst()
                    .orElse(SpecialtyInfo.newBuilder().setName("NOT_FOUND").setAvailableSlots(0).build());
            responseObserver.onNext(info);
            responseObserver.onCompleted();
        }
    }
}
