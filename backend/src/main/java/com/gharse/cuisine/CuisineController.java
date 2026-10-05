package com.gharse.cuisine;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuisines")
public class CuisineController {

    public record CuisineResponse(UUID id, String name) {}

    private final CuisineRepository repo;

    public CuisineController(CuisineRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<CuisineResponse> list() {
        return repo.findAllByOrderByNameAsc().stream().map(c -> new CuisineResponse(c.getId(), c.getName())).toList();
    }
}
