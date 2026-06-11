package edu.eci.arsw.evolucion.movie_microservices;

import edu.eci.arsw.evolucion.movie_grpc.MovieRequest;
import edu.eci.arsw.evolucion.movie_grpc.MovieResponse;
import edu.eci.arsw.evolucion.movie_grpc.MovieServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

/**
 * Cliente que agrega los tres microservicios de películas:
 *   MovieService       → localhost:50051
 *   ReviewService      → localhost:50055
 *   RecommendationService → localhost:50056
 */
public class MovieMicroClient {

    public static void main(String[] args) {
        ManagedChannel movieChannel  = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        ManagedChannel reviewChannel = ManagedChannelBuilder.forAddress("localhost", 50055).usePlaintext().build();
        ManagedChannel recChannel    = ManagedChannelBuilder.forAddress("localhost", 50056).usePlaintext().build();

        MovieServiceGrpc.MovieServiceBlockingStub          movieStub  = MovieServiceGrpc.newBlockingStub(movieChannel);
        ReviewServiceGrpc.ReviewServiceBlockingStub        reviewStub = ReviewServiceGrpc.newBlockingStub(reviewChannel);
        RecommendationServiceGrpc.RecommendationServiceBlockingStub recStub =
                RecommendationServiceGrpc.newBlockingStub(recChannel);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el ID de la pelicula (1-3): ");
        int id = scanner.nextInt();

        MovieResponse movie = movieStub.getMovie(MovieRequest.newBuilder().setId(id).build());
        if (!movie.getFound()) {
            System.out.println("Pelicula no encontrada");
        } else {
            System.out.println("\nPelicula : " + movie.getTitle());
            System.out.println("Director : " + movie.getDirector());
            System.out.println("Anio     : " + movie.getYear());

            System.out.println("\nResenias:");
            reviewStub.getReviews(ReviewRequest.newBuilder().setMovieId(id).build())
                    .getReviewsList()
                    .forEach(r -> System.out.println("  - " + r.getComment() + " | Rating: " + r.getRating()));

            System.out.println("\nRecomendaciones:");
            recStub.getRecommendations(RecommendationRequest.newBuilder().setMovieId(id).build())
                    .getTitlesList()
                    .forEach(t -> System.out.println("  - " + t));
        }

        movieChannel.shutdown();
        reviewChannel.shutdown();
        recChannel.shutdown();
        scanner.close();
    }
}
