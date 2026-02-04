package com.umdev.directory.constant;

/**
 * Enum representing the type of contact.
 *
 * ContactTypeKey is used to identify and categorize types of contact points
 * within the directory system. Each constant in this enum corresponds to a
 * specific type of contact that can be associated with entities or operations
 * within the application.
 *
 * The available types are:
 * - SCE: Represents a specific contact entity.
 * - MEC: Represents a main or major contact entity.
 *
 * The enum provides override for the toString method to return the name
 * of the constant as a string.
 */
public enum ContactTypeKey {
    SCE,
    MEC,
    WBH,
    EML;

    @Override
    public String toString() {
        return this.name();
    }
}
