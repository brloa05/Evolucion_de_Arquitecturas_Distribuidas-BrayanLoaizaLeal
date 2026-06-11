package edu.eci.arsw.evolucion.eciciencia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LibraryGrpcServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50072)
                .addService(new LibraryServiceImpl())
                .build();
        server.start();
        System.out.println("LibraryService gRPC iniciado en puerto 50072");
        server.awaitTermination();
    }

    static class LibraryServiceImpl extends LibraryServiceGrpc.LibraryServiceImplBase {
        private final Map<String, Book> books = new HashMap<>();

        public LibraryServiceImpl() {
            books.put("LIB-001", Book.newBuilder().setBookId("LIB-001")
                    .setTitle("Clean Code").setAuthor("Robert C. Martin").setAvailable(true).build());
            books.put("LIB-002", Book.newBuilder().setBookId("LIB-002")
                    .setTitle("Designing Data-Intensive Applications").setAuthor("Martin Kleppmann").setAvailable(true).build());
            books.put("LIB-003", Book.newBuilder().setBookId("LIB-003")
                    .setTitle("Microservices Patterns").setAuthor("Chris Richardson").setAvailable(true).build());
            books.put("LIB-004", Book.newBuilder().setBookId("LIB-004")
                    .setTitle("Domain-Driven Design").setAuthor("Eric Evans").setAvailable(true).build());
            books.put("LIB-005", Book.newBuilder().setBookId("LIB-005")
                    .setTitle("The Pragmatic Programmer").setAuthor("Hunt & Thomas").setAvailable(true).build());
        }

        @Override
        public void searchBooks(SearchRequest request, StreamObserver<BookList> responseObserver) {
            String q = request.getQuery().toLowerCase();
            List<Book> results = books.values().stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(q) || b.getAuthor().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            responseObserver.onNext(BookList.newBuilder().addAllBooks(results).build());
            responseObserver.onCompleted();
        }

        @Override
        public void borrowBook(BorrowRequest request, StreamObserver<BorrowResponse> responseObserver) {
            BorrowResponse response;
            String bookId = request.getBookId();
            if (!books.containsKey(bookId)) {
                response = BorrowResponse.newBuilder().setSuccess(false).setMessage("Libro no encontrado").build();
            } else if (!books.get(bookId).getAvailable()) {
                response = BorrowResponse.newBuilder().setSuccess(false).setMessage("Libro no disponible").build();
            } else {
                books.put(bookId, books.get(bookId).toBuilder().setAvailable(false).build());
                String dueDate = LocalDate.now().plusDays(14).toString();
                response = BorrowResponse.newBuilder().setSuccess(true)
                        .setMessage("Prestamo registrado para " + request.getStudentId())
                        .setDueDate(dueDate).build();
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
