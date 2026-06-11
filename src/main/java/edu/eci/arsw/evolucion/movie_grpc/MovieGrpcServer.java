package edu.eci.arsw.evolucion.movie_grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

public class MovieGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50051)
                .addService(new MovieServiceImpl())
                .build();
        server.start();
        System.out.println("Movie gRPC Server iniciado en puerto 50051");
        server.awaitTermination();
    }

    static class MovieServiceImpl extends MovieServiceGrpc.MovieServiceImplBase {
        private final Map<Integer, MovieResponse> movies = new HashMap<>();

        public MovieServiceImpl() {
            movies.put(1, MovieResponse.newBuilder().setId(1).setTitle("Interstellar")
                    .setDirector("Christopher Nolan").setYear(2014).setFound(true).build());
            movies.put(2, MovieResponse.newBuilder().setId(2).setTitle("Matrix")
                    .setDirector("Wachowski").setYear(1999).setFound(true).build());
            movies.put(3, MovieResponse.newBuilder().setId(3).setTitle("Inception")
                    .setDirector("Christopher Nolan").setYear(2010).setFound(true).build());
        }

        @Override
        public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
            MovieResponse response = movies.getOrDefault(request.getId(),
                    MovieResponse.newBuilder().setId(request.getId()).setFound(false).build());
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
