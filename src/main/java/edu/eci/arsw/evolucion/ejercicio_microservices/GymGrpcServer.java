package edu.eci.arsw.evolucion.ejercicio_microservices;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GymGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50061)
                .addService(new GymServiceImpl())
                .build();
        server.start();
        System.out.println("GymService gRPC iniciado en puerto 50061");
        server.awaitTermination();
    }

    static class GymServiceImpl extends GymServiceGrpc.GymServiceImplBase {
        private final Map<String, Boolean> slots = new LinkedHashMap<>();

        public GymServiceImpl() {
            slots.put("07:00 - 08:00", true);
            slots.put("08:00 - 09:00", true);
            slots.put("12:00 - 13:00", true);
            slots.put("17:00 - 18:00", true);
            slots.put("18:00 - 19:00", true);
        }

        @Override
        public void getAvailableSlots(EmptyGymRequest request, StreamObserver<SlotList> responseObserver) {
            List<String> available = new ArrayList<>();
            slots.forEach((slot, free) -> { if (free) available.add(slot); });
            responseObserver.onNext(SlotList.newBuilder().addAllSlots(available).build());
            responseObserver.onCompleted();
        }

        @Override
        public void reserveSlot(SlotRequest request, StreamObserver<SlotResponse> responseObserver) {
            SlotResponse response;
            if (!slots.containsKey(request.getTimeSlot())) {
                response = SlotResponse.newBuilder().setSuccess(false).setMessage("Franja no existe").build();
            } else if (!slots.get(request.getTimeSlot())) {
                response = SlotResponse.newBuilder().setSuccess(false).setMessage("Franja ya reservada").build();
            } else {
                slots.put(request.getTimeSlot(), false);
                response = SlotResponse.newBuilder().setSuccess(true)
                        .setMessage("Reserva exitosa: " + request.getStudentId() + " → " + request.getTimeSlot()).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
