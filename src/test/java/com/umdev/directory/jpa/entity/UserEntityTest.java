package com.umdev.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    @DisplayName("Get ID")
    void getId() {
        UserEntity userEntity = new UserEntity();
        UUID id = UUID.randomUUID();
        userEntity.setId(id);
        assertEquals(id, userEntity.getId());
    }

    @Test
    @DisplayName("Set ID to Null")
    void setIdToNull() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(null);
        assertNull(userEntity.getId());
    }
}
