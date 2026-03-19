package com.library.controller;

import java.util.Map;

import com.library.dto.BookRequest;
import com.library.model.Book;
import com.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {

        if (title != null) {
            return ResponseEntity.ok(bookService.searchByTitle(title));
        }
        if (category != null) {
            return ResponseEntity.ok(bookService.searchByCategory(category));
        }
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Book> addBook(@Valid @RequestBody BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(request.getCategory());
        return ResponseEntity.ok(bookService.addBook(book));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Book> updateBook(@PathVariable Long id,
                                           @Valid @RequestBody BookRequest request) {
        Book updated = new Book();
        updated.setTitle(request.getTitle());
        updated.setAuthor(request.getAuthor());
        updated.setCategory(request.getCategory());
        return ResponseEntity.ok(bookService.updateBook(id, updated));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(Map.of("message", "Book deleted successfully"));
    }
}