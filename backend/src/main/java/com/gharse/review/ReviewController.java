package com.gharse.review;

import com.gharse.auth.AuthUser;
import com.gharse.common.PageResponse;
import com.gharse.review.ReviewDtos.ReviewRequest;
import com.gharse.review.ReviewDtos.ReviewResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/orders/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable UUID id, @Valid @RequestBody ReviewRequest r) {
        return service.create(AuthUser.current().id(), id, r);
    }

    @GetMapping("/cooks/{id}/reviews")
    public PageResponse<ReviewResponse> ofCook(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return service.ofCook(id, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }
}
