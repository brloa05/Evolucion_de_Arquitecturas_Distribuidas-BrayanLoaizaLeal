package edu.eci.arsw.evolucion.eciciencia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StudentGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50070)
                .addService(new StudentServiceImpl())
                .build();
        server.start();
        System.out.println("StudentService gRPC iniciado en puerto 50070");
        server.awaitTermination();
    }

    static class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {
        private final Map<String, StudentResponse> students = new HashMap<>();

        public StudentServiceImpl() {
            students.put("EST-001", StudentResponse.newBuilder()
                    .setStudentId("EST-001").setName("Ana Gomez").setEmail("ana.gomez@eci.edu.co")
                    .setMajor("Ingenieria de Sistemas").setFound(true).build());
            students.put("EST-002", StudentResponse.newBuilder()
                    .setStudentId("EST-002").setName("Carlos Perez").setEmail("carlos.perez@eci.edu.co")
                    .setMajor("Ingenieria Civil").setFound(true).build());
            students.put("EST-003", StudentResponse.newBuilder()
                    .setStudentId("EST-003").setName("Laura Torres").setEmail("laura.torres@eci.edu.co")
                    .setMajor("Matematicas").setFound(true).build());
        }

        @Override
        public void getStudent(StudentRequest request, StreamObserver<StudentResponse> responseObserver) {
            StudentResponse res = students.getOrDefault(request.getStudentId(),
                    StudentResponse.newBuilder().setFound(false).build());
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        }

        @Override
        public void registerStudent(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
            String id = "EST-" + String.format("%03d", students.size() + 1);
            students.put(id, StudentResponse.newBuilder()
                    .setStudentId(id).setName(request.getName())
                    .setEmail(request.getEmail()).setMajor(request.getMajor()).setFound(true).build());
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setStudentId(id).setSuccess(true)
                    .setMessage("Estudiante registrado exitosamente con ID: " + id).build());
            responseObserver.onCompleted();
        }
    }
}
