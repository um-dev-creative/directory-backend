package com.prx.directory.kafka.to;

public record Recipient(
        String name,
        String email,
        String alias
) {

    @Override
    public String toString() {
        return "Recipient{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", alias='" + alias + '\'' +
                '}';
    }
}
