package com.prx.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timezone", schema = "directory_site")
public class TimezoneEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -5704966862650157440L;
    @Id
    @ColumnDefault("directory_site.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "utc_offset", nullable = false)
    @Convert(converter = TimezoneEntity.DurationToStringConverter.class)
    private Duration utcOffset;

    @Size(max = 10)
    @NotNull
    @Column(name = "abbreviation", nullable = false, length = 10)
    private String abbreviation;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @NotNull
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    public TimezoneEntity() {
        // Default constructor
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Duration getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(Duration utcOffset) {
        this.utcOffset = utcOffset;
    }

    /**
     * JPA AttributeConverter to map Duration to String (ISO-8601) and back.
     */
    public static class DurationToStringConverter implements jakarta.persistence.AttributeConverter<Duration, String> {
        private static final int MAX_LENGTH = 3;

        @Override
        public String convertToDatabaseColumn(Duration attribute) {
            return attribute == null ? null : attribute.toString();
        }

        @Override
        public Duration convertToEntityAttribute(String dbData) {
            if (dbData == null) return null;
            try {
                return Duration.parse(dbData); // ISO-8601
            } catch (Exception e) {
                // Try HH:mm:ss
                String[] parts = dbData.split(":");
                if (parts.length == MAX_LENGTH) {
                    long hours = Long.parseLong(parts[0]);
                    long minutes = Long.parseLong(parts[1]);
                    long seconds = Long.parseLong(parts[2]);
                    return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
                }
                throw new IllegalArgumentException("Cannot parse duration: " + dbData, e);
            }
        }
    }
}
