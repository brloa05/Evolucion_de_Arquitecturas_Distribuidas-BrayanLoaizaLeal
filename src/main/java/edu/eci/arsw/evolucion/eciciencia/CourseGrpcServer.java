package edu.eci.arsw.evolucion.eciciencia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50071)
                .addService(new CourseServiceImpl())
                .build();
        server.start();
        System.out.println("CourseService gRPC iniciado en puerto 50071");
        server.awaitTermination();
    }

    static class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {
        private final Map<String, Course> catalog = new HashMap<>();
        private final Map<String, List<String>> enrollments = new HashMap<>();

        public CourseServiceImpl() {
            catalog.put("ARSW", Course.newBuilder().setCourseId("ARSW")
                    .setName("Arquitecturas de Software").setProfessor("Prof. Martinez").setAvailableSlots(25).build());
            catalog.put("CVDS", Course.newBuilder().setCourseId("CVDS")
                    .setName("Ciclos de Vida del Desarrollo").setProfessor("Prof. Rodriguez").setAvailableSlots(30).build());
            catalog.put("IETI", Course.newBuilder().setCourseId("IETI")
                    .setName("Introduccion a Tecnologias de Internet").setProfessor("Prof. Lopez").setAvailableSlots(20).build());
        }

        @Override
        public void getCourses(EmptyCourseRequest request, StreamObserver<CourseList> responseObserver) {
            responseObserver.onNext(CourseList.newBuilder().addAllCourses(catalog.values()).build());
            responseObserver.onCompleted();
        }

        @Override
        public void enrollStudent(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
            EnrollResponse response;
            String courseId = request.getCourseId();

            if (!catalog.containsKey(courseId)) {
                response = EnrollResponse.newBuilder().setSuccess(false).setMessage("Curso no encontrado").build();
            } else {
                Course course = catalog.get(courseId);
                List<String> enrolled = enrollments.computeIfAbsent(courseId, k -> new ArrayList<>());
                if (enrolled.contains(request.getStudentId())) {
                    response = EnrollResponse.newBuilder().setSuccess(false).setMessage("Estudiante ya matriculado").build();
                } else if (course.getAvailableSlots() == 0) {
                    response = EnrollResponse.newBuilder().setSuccess(false).setMessage("Sin cupos disponibles").build();
                } else {
                    enrolled.add(request.getStudentId());
                    catalog.put(courseId, course.toBuilder().setAvailableSlots(course.getAvailableSlots() - 1).build());
                    response = EnrollResponse.newBuilder().setSuccess(true)
                            .setMessage("Matricula exitosa en " + course.getName()).build();
                }
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
