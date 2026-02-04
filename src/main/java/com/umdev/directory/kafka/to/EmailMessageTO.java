package com.umdev.directory.kafka.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umdev.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EmailMessageTO(
        @JsonProperty("template_defined_id")
        UUID templateDefinedId,
        @JsonProperty("user_id")
        UUID userId,
        @JsonProperty("from")
        String from,
        @JsonProperty("to")
        List<Recipient> to,
        @JsonProperty("cc")
        List<Recipient> cc,
        @JsonProperty("subject")
        String subject,
        @JsonProperty("body")
        String body,
        @JsonProperty("send_date")
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime sendDate,
        @JsonProperty("params")
        Map<String, Object> params
) {

    @Override
    public String toString() {
        return "EmailMessageTO{" +
                "template_defined_id='" + templateDefinedId + '\'' +
                ", user_id='" + userId + '\'' +
                ", from='" + from + '\'' +
                ", to=" + to +
                ", cc=" + cc +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", send_date='" + sendDate + '\'' +
                ", params=" + params +
                '}';
    }
}
