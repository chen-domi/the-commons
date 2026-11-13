package com.thecommons.backend.nlp;

import java.security.Principal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
public class ItemDescriptionController {
    private final ItemDescriptionService service;

    public ItemDescriptionController(ItemDescriptionService service) { this.service = service; }

    public record ParseRequest(@NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 255) String organization) {}
    public record Availability(boolean available) {}

    @GetMapping("/api/nlp/status")
    public Availability availability() { return new Availability(service.isAvailable()); }

    @PostMapping("/api/inventory/parse-description")
    public ItemDraft parse(Principal principal, @Valid @RequestBody ParseRequest request) {
        return service.parse(principal.getName(), request.organization(), request.description());
    }
}
