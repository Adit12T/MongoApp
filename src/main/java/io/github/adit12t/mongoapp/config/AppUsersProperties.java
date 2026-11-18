package io.github.adit12t.mongoapp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Credentials for the two built-in accounts, read from the environment. The app refuses to start without them. */
@Validated
@ConfigurationProperties("app.security")
public record AppUsersProperties(
        @NotBlank @Size(min = 12) String userPassword,
        @NotBlank @Size(min = 12) String adminPassword) {
}
