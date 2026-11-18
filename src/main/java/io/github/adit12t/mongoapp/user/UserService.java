package io.github.adit12t.mongoapp.user;

import io.github.adit12t.mongoapp.user.UserDtos.AgeStats;
import io.github.adit12t.mongoapp.user.UserDtos.CreateUserRequest;
import io.github.adit12t.mongoapp.user.UserDtos.UserEvent;
import io.github.adit12t.mongoapp.user.UserDtos.UserResponse;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class UserService {

    private final UserProfileRepository users;
    private final ReactiveMongoTemplate template;
    /** Fan-out of create/update/delete events to every connected stream subscriber. */
    private final Sinks.Many<UserEvent> events = Sinks.many().multicast().directBestEffort();

    public UserService(UserProfileRepository users, ReactiveMongoTemplate template) {
        this.users = users;
        this.template = template;
    }

    public Mono<UserResponse> create(CreateUserRequest request) {
        return users.save(new UserProfile(request.name().trim(), request.age()))
                .onErrorMap(DuplicateKeyException.class,
                        e -> new ResponseStatusException(HttpStatus.CONFLICT, "A user with this name already exists."))
                .doOnNext(saved -> publish("CREATED", saved.getName()))
                .map(UserResponse::from);
    }

    public Mono<UserResponse> get(String name) {
        return find(name).map(UserResponse::from);
    }

    public Flux<UserResponse> list(Integer minAge, Integer maxAge, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name"));
        Flux<UserProfile> result = minAge == null && maxAge == null
                ? users.findAllBy(pageable)
                : users.findByAgeBetween(minAge == null ? -1 : minAge - 1, maxAge == null ? 151 : maxAge + 1, pageable);
        return result.map(UserResponse::from);
    }

    public Mono<UserResponse> updateAge(String name, int age) {
        return find(name)
                .map(user -> user.withAge(age))
                .flatMap(users::save)
                .doOnNext(saved -> publish("UPDATED", saved.getName()))
                .map(UserResponse::from);
    }

    public Mono<Void> delete(String name) {
        return find(name).flatMap(users::delete).then(Mono.fromRunnable(() -> publish("DELETED", name)));
    }

    public Mono<Long> deleteAll() {
        return users.count().flatMap(count -> users.deleteAll().thenReturn(count))
                .doOnNext(count -> publish("CLEARED", null));
    }

    public Mono<AgeStats> stats() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group().count().as("count").avg("age").as("averageAge")
                        .min("age").as("minAge").max("age").as("maxAge"));
        return template.aggregate(aggregation, UserProfile.class, Document.class)
                .next()
                .map(doc -> new AgeStats(((Number) doc.get("count")).longValue(),
                        Math.round(((Number) doc.get("averageAge")).doubleValue() * 10) / 10.0,
                        ((Number) doc.get("minAge")).intValue(), ((Number) doc.get("maxAge")).intValue()))
                .defaultIfEmpty(new AgeStats(0, 0, 0, 0));
    }

    public Flux<UserEvent> events() {
        return events.asFlux();
    }

    private Mono<UserProfile> find(String name) {
        return users.findByName(name)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No user named " + name)));
    }

    private void publish(String type, String name) {
        events.tryEmitNext(new UserEvent(type, name));
    }
}
