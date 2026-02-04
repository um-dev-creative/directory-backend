package com.umdev.directory.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Entity class representing a user.
 * This class is mapped to the "user" table in the "general" schema.
 * It includes a UUID as the primary key.
 */
@Entity
@Table(name = "user", schema = "general")
public class UserEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 3926074203487008986L;

    /**
     * The unique identifier for the user.
     * This field is automatically generated using the UUID generation strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ColumnDefault("general.uuid_generate_v4()")
    @Column(name = "id", nullable = false)
    private UUID id;

    public UserEntity() {
        // Default constructor
    }

    /**
     * Gets the unique identifier for the user.
     *
     * @return the unique identifier for the user
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the user.
     *
     * @param id the unique identifier for the user
     */
    public void setId(UUID id) {
        this.id = id;
    }

}
