package io.github.adit12t.mongoapp.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserProfileRepository extends ReactiveMongoRepository<UserProfile, String> {

    Mono<UserProfile> findByName(String name);

    Flux<UserProfile> findByAgeBetween(int minAgeExclusive, int maxAgeExclusive, Pageable pageable);

    Flux<UserProfile> findAllBy(Pageable pageable);
}
