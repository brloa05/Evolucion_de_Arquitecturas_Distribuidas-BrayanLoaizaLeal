package edu.eci.arsw.evolucion.movie_microservices;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50055)
                .addService(new ReviewServiceImpl())
                .build();
        server.start();
        System.out.println("ReviewService gRPC iniciado en puerto 50055");
        server.awaitTermination();
    }

    static class ReviewServiceImpl extends ReviewServiceGrpc.ReviewServiceImplBase {
        private final Map<Integer, List<Review>> reviews = new HashMap<>();

        public ReviewServiceImpl() {
            reviews.put(1, Arrays.asList(
                    Review.newBuilder().setAuthor("Juan").setComment("Excelente pelicula de ciencia ficcion").setRating(5).build(),
                    Review.newBuilder().setAuthor("Maria").setComment("Visualmente impresionante").setRating(4).build()
            ));
            reviews.put(2, Arrays.asList(
                    Review.newBuilder().setAuthor("Carlos").setComment("Un clasico del cyberpunk").setRating(5).build()
            ));
            reviews.put(3, Arrays.asList(
                    Review.newBuilder().setAuthor("Ana").setComment("Mente-abiertamente alucinante").setRating(5).build(),
                    Review.newBuilder().setAuthor("Pedro").setComment("Muy compleja pero excelente").setRating(4).build()
            ));
        }

        @Override
        public void getReviews(ReviewRequest request, StreamObserver<ReviewList> responseObserver) {
            List<Review> list = reviews.getOrDefault(request.getMovieId(), List.of());
            responseObserver.onNext(ReviewList.newBuilder().addAllReviews(list).build());
            responseObserver.onCompleted();
        }
    }
}
