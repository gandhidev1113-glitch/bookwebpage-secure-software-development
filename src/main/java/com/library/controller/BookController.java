package com.library.controller;

import com.library.config.SecurityLogger;
import com.library.dto.BookRequest;
import com.library.model.Book;
import com.library.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private final BookService bookService;
    private final SecurityLogger securityLogger;

    public BookController(BookService bookService,
                          SecurityLogger securityLogger) {
        this.bookService = bookService;
        this.securityLogger = securityLogger;
    }

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> search(
            @RequestParam(required = false) @Size(max = 200) String title,
            @RequestParam(required = false) @Size(max = 100) String category) {
        if (title != null) {
            return ResponseEntity.ok(bookService.searchByTitle(title));
        }
        if (category != null) {
            return ResponseEntity.ok(bookService.searchByCategory(category));
        }
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<Book> addBook(@Valid @RequestBody BookRequest request,
                                        Authentication authentication) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(request.getCategory());
        Book saved = bookService.addBook(book);
        securityLogger.logBookAdded(authentication.getName(), saved.getTitle());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<Book> updateBook(@PathVariable @Positive Long id,
                                           @Valid @RequestBody BookRequest request) {
        Book updated = new Book();
        updated.setTitle(request.getTitle());
        updated.setAuthor(request.getAuthor());
        updated.setCategory(request.getCategory());
        return ResponseEntity.ok(bookService.updateBook(id, updated));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<?> deleteBook(@PathVariable @Positive Long id,
                                        Authentication authentication) {
        bookService.deleteBook(id);
        securityLogger.logBookDeleted(authentication.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Book deleted successfully"));
    }
}
