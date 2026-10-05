package com.gharse.availability;

import com.gharse.auth.AuthUser;
import com.gharse.availability.BatchDtos.BatchRequest;
import com.gharse.availability.BatchDtos.BatchResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping("/cooks/{cookId}/batches")
    public List<BatchResponse> cookBatches(@PathVariable UUID cookId) {
        return service.upcomingOf(cookId);
    }

    /** Marketplace-wide open batches, for "Available Today" and "Limited Batch" sections. */
    @GetMapping("/batches")
    public List<BatchResponse> discover(@RequestParam(defaultValue = "false") boolean today,
                                        @RequestParam(defaultValue = "false") boolean limited,
                                        @RequestParam(defaultValue = "12") int limit) {
        return service.discover(today, limited, Math.min(Math.max(limit, 1), 50));
    }

    @GetMapping("/cook/batches")
    public List<BatchResponse> mine() {
        return service.mine(AuthUser.current().id());
    }

    @PostMapping("/cook/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse create(@Valid @RequestBody BatchRequest r) {
        return service.create(AuthUser.current().id(), r);
    }

    @PutMapping("/cook/batches/{id}")
    public BatchResponse update(@PathVariable UUID id, @Valid @RequestBody BatchRequest r) {
        return service.update(AuthUser.current().id(), id, r);
    }
}
