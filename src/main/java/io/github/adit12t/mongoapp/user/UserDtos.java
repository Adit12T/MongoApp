package io.github.adit12t.mongoapp.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 50) @Pattern(regexp = "^[\\p{L}0-9 .'-]+$", message = "contains unsupported characters")
            String name,
            @Min(0) @Max(150) int age) {
    }

    public record UpdateAgeRequest(@Min(0) @Max(150) int age) {
    }

    public record UserResponse(String id, String name, int age, Instant createdAt, Instant updatedAt) {
        static UserResponse from(UserProfile user) {
            return new UserResponse(user.getId(), user.getName(), user.getAge(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    public record AgeStats(long count, double averageAge, int minAge, int maxAge) {
    }

    public record UserEvent(String type, String name) {
    }
}
