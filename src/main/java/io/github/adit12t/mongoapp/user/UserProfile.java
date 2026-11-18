package io.github.adit12t.mongoapp.user;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("user_profiles")
public class UserProfile {

    @Id
    private String id;
    @Version
    private Long version;
    @Indexed(unique = true)
    private String name;
    @Indexed
    private int age;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    protected UserProfile() {
    }

    public UserProfile(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public UserProfile withAge(int newAge) {
        this.age = newAge;
        return this;
    }

    public String getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
