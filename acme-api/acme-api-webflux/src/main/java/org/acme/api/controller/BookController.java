package org.acme.api.controller;

import org.acme.api.service.BookService;
import org.acme.persistence.r2dbc.Book;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book management API (Reactive)")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new book", description = "Creates a new book in the system (reactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing x-dn header")
    })
    public Mono<Book> create(@RequestBody Book book) {
        return bookService.create(book);
    }

    @GetMapping
    @Operation(summary = "Get all books", description = "Retrieves all books from the system (reactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of books"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing x-dn header")
    })
    public Flux<Book> findAll() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieves a specific book by its ID (reactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing x-dn header")
    })
    public Mono<Book> findById(
            @Parameter(description = "Book ID", required = true) @PathVariable(name = "id") Long id) {
        return bookService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book", description = "Updates an existing book by its ID (reactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book updated successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing x-dn header")
    })
    public Mono<Book> update(
            @Parameter(description = "Book ID", required = true) @PathVariable(name = "id") Long id,
            @RequestBody Book book) {
        return bookService.update(id, book);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", description = "Deletes a book by its ID (reactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing x-dn header")
    })
    public Mono<Void> delete(
            @Parameter(description = "Book ID", required = true) @PathVariable(name = "id") Long id) {
        return bookService.delete(id);
    }
}
