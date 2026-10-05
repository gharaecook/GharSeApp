package com.gharse.menu;

import com.gharse.auth.AuthUser;
import com.gharse.menu.DishDtos.DishRequest;
import com.gharse.menu.DishDtos.DishResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DishController {

    private final DishService service;

    public DishController(DishService service) {
        this.service = service;
    }

    @GetMapping("/cooks/{cookId}/dishes")
    public List<DishResponse> cookDishes(@PathVariable UUID cookId) {
        return service.publicDishesOf(cookId);
    }

    @GetMapping("/dishes/{id}")
    public DishResponse dish(@PathVariable UUID id) {
        return service.publicDish(id);
    }

    @GetMapping("/cook/dishes")
    public List<DishResponse> mine() {
        return service.myDishes(AuthUser.current().id());
    }

    @PostMapping("/cook/dishes")
    @ResponseStatus(HttpStatus.CREATED)
    public DishResponse create(@Valid @RequestBody DishRequest r) {
        return service.create(AuthUser.current().id(), r);
    }

    @PutMapping("/cook/dishes/{id}")
    public DishResponse update(@PathVariable UUID id, @Valid @RequestBody DishRequest r) {
        return service.update(AuthUser.current().id(), id, r);
    }

    @DeleteMapping("/cook/dishes/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(AuthUser.current().id(), id);
        return ResponseEntity.noContent().build();
    }
}
