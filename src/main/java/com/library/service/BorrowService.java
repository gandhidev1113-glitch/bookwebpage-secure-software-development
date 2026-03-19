package com.library.service;

import com.library.model.Book;
import com.library.model.BorrowRequest;
import com.library.model.BorrowRequest.BorrowStatus;
import com.library.model.User;
import com.library.repository.BorrowRequestRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BorrowService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final BookService bookService;

    public BorrowService(BorrowRequestRepository borrowRequestRepository,
                         BookService bookService) {
        this.borrowRequestRepository = borrowRequestRepository;
        this.bookService = bookService;
    }

    public BorrowRequest requestBorrow(User user, Long bookId) {
        Book book = bookService.getBookById(bookId);

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available");
        }

        BorrowRequest request = new BorrowRequest();
        request.setUser(user);
        request.setBook(book);
        request.setStatus(BorrowStatus.PENDING);
        return borrowRequestRepository.save(request);
    }

    public BorrowRequest approveRequest(Long requestId) {
        BorrowRequest request = getRequestById(requestId);
        request.setStatus(BorrowStatus.APPROVED);
        request.getBook().setAvailable(false); // Mark book as unavailable
        return borrowRequestRepository.save(request);
    }

    public BorrowRequest rejectRequest(Long requestId) {
        BorrowRequest request = getRequestById(requestId);
        request.setStatus(BorrowStatus.REJECTED);
        return borrowRequestRepository.save(request);
    }

    public List<BorrowRequest> getRequestsByUser(User user) {
        return borrowRequestRepository.findByUser(user);
    }

    public List<BorrowRequest> getPendingRequests() {
        return borrowRequestRepository.findByStatus(BorrowStatus.PENDING);
    }

    private BorrowRequest getRequestById(Long id) {
        return borrowRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Borrow request not found"));
    }
}