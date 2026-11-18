package io.github.adit12t.mongoapp;

import io.github.adit12t.mongoapp.user.UserDtos.UserEvent;
import io.github.adit12t.mongoapp.user.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

@SpringBootTest(properties = {
        "app.security.user-password=user-password-for-tests",
        "app.security.admin-password=admin-password-for-tests"})
@AutoConfigureWebTestClient
@Testcontainers
class MongoAppIntegrationTests {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private WebTestClient client;

    @Autowired
    private UserProfileRepository repository;

    private WebTestClient asUser() {
        return client.mutate().defaultHeaders(h -> h.setBasicAuth("user", "user-password-for-tests")).build();
    }

    private WebTestClient asAdmin() {
        return client.mutate().defaultHeaders(h -> h.setBasicAuth("admin", "admin-password-for-tests")).build();
    }

    private void create(String name, int age) {
        asUser().post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"%s\",\"age\":%d}".formatted(name, age))
                .exchange().expectStatus().isCreated();
    }

    @BeforeEach
    void clean() {
        repository.deleteAll().block();
    }

    @Test
    void requiresAuthentication() {
        client.get().uri("/api/v1/users").exchange().expectStatus().isUnauthorized();
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void createReadUpdateDelete() {
        create("Asha", 31);

        asUser().get().uri("/api/v1/users/Asha").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.age").isEqualTo(31);
        asUser().patch().uri("/api/v1/users/Asha").contentType(MediaType.APPLICATION_JSON).bodyValue("{\"age\":32}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.age").isEqualTo(32);
        asUser().delete().uri("/api/v1/users/Asha").exchange().expectStatus().isNoContent();
        asUser().get().uri("/api/v1/users/Asha").exchange().expectStatus().isNotFound();
    }

    @Test
    void rejectsDuplicatesAndInvalidInput() {
        create("Ravi", 40);
        asUser().post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Ravi\",\"age\":41}").exchange().expectStatus().isEqualTo(409);
        asUser().post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"\",\"age\":200}").exchange().expectStatus().isBadRequest();
        asUser().post().uri("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"{$gt: ''}\",\"age\":20}").exchange().expectStatus().isBadRequest();
    }

    @Test
    void filtersByAgeRangeAndComputesStats() {
        create("Ana", 20);
        create("Ben", 30);
        create("Cai", 40);

        asUser().get().uri("/api/v1/users?minAge=25&maxAge=40").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Ben");
        asUser().get().uri("/api/v1/users/stats").exchange()
                .expectBody().jsonPath("$.count").isEqualTo(3).jsonPath("$.averageAge").isEqualTo(30.0)
                .jsonPath("$.minAge").isEqualTo(20).jsonPath("$.maxAge").isEqualTo(40);
    }

    @Test
    void onlyAdminCanDeleteEveryone() {
        create("Dev", 25);
        asUser().delete().uri("/api/v1/users").exchange().expectStatus().isForbidden();
        asAdmin().delete().uri("/api/v1/users").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.deleted").isEqualTo(1);
    }

    @Test
    void streamsChangesAsServerSentEvents() {
        Flux<ServerSentEvent<UserEvent>> stream = asUser().get().uri("/api/v1/users/events")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<UserEvent>>() { })
                .getResponseBody()
                .filter(sse -> sse.data() != null);

        StepVerifier.create(stream)
                .then(() -> create("Eva", 28))
                .assertNext(sse -> {
                    org.assertj.core.api.Assertions.assertThat(sse.data().type()).isEqualTo("CREATED");
                    org.assertj.core.api.Assertions.assertThat(sse.data().name()).isEqualTo("Eva");
                })
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }
}
