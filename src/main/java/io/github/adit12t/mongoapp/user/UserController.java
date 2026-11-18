package io.github.adit12t.mongoapp.user;

import io.github.adit12t.mongoapp.user.UserDtos.AgeStats;
import io.github.adit12t.mongoapp.user.UserDtos.CreateUserRequest;
import io.github.adit12t.mongoapp.user.UserDtos.UpdateAgeRequest;
import io.github.adit12t.mongoapp.user.UserDtos.UserEvent;
import io.github.adit12t.mongoapp.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "basicAuth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public Mono<ResponseEntity<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request)
                .map(user -> ResponseEntity.created(URI.create("/api/v1/users/" + user.name())).body(user));
    }

    @GetMapping
    @Operation(summary = "List users, optionally within an age range")
    public Flux<UserResponse> list(@RequestParam(required = false) @Min(0) @Max(150) Integer minAge,
                                   @RequestParam(required = false) @Min(0) @Max(150) Integer maxAge,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(minAge, maxAge, page, size);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a user by name")
    public Mono<UserResponse> get(@PathVariable String name) {
        return service.get(name);
    }

    @PatchMapping("/{name}")
    @Operation(summary = "Change a user's age")
    public Mono<UserResponse> updateAge(@PathVariable String name, @Valid @RequestBody UpdateAgeRequest request) {
        return service.updateAge(name, request.age());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user")
    public Mono<Void> delete(@PathVariable String name) {
        return service.delete(name);
    }

    @DeleteMapping
    @Operation(summary = "Delete every user (admin only)")
    public Mono<Map<String, Long>> deleteAll() {
        return service.deleteAll().map(count -> Map.of("deleted", count));
    }

    @GetMapping("/stats")
    @Operation(summary = "Count and age statistics, computed by a MongoDB aggregation")
    public Mono<AgeStats> stats() {
        return service.stats();
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live stream of user changes (Server-Sent Events)")
    public Flux<ServerSentEvent<UserEvent>> events() {
        Flux<ServerSentEvent<UserEvent>> changes = service.events()
                .map(event -> ServerSentEvent.builder(event).event("user").build());
        Flux<ServerSentEvent<UserEvent>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<UserEvent>builder().comment("keep-alive").build());
        // Send a comment straight away so the response starts and clients know they are connected.
        return Flux.merge(changes, heartbeat)
                .startWith(ServerSentEvent.<UserEvent>builder().comment("connected").build());
    }
}
