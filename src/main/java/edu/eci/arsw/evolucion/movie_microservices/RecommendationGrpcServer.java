package edu.eci.arsw.evolucion.movie_microservices;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50056)
                .addService(new RecommendationServiceImpl())
                .build();
        server.start();
        System.out.println("RecommendationService gRPC iniciado en puerto 50056");
        server.awaitTermination();
    }

    static class RecommendationServiceImpl extends RecommendationServiceGrpc.RecommendationServiceImplBase {
        private final Map<Integer, List<String>> recommendations = new HashMap<>();

        public RecommendationServiceImpl() {
            recommendations.put(1, Arrays.asList("Inception", "Contact", "2001: A Space Odyssey"));
            recommendations.put(2, Arrays.asList("Blade Runner 2049", "The Matrix Reloaded", "Ghost in the Shell"));
            recommendations.put(3, Arrays.asList("Interstellar", "Shutter Island", "Memento"));
        }

        @Override
        public void getRecommendations(RecommendationRequest request,
                                       StreamObserver<RecommendationList> responseObserver) {
            List<String> titles = recommendations.getOrDefault(request.getMovieId(), List.of());
            responseObserver.onNext(RecommendationList.newBuilder().addAllTitles(titles).build());
            responseObserver.onCompleted();
        }
    }
}
