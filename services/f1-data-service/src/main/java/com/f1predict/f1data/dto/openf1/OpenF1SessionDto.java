package com.f1predict.f1data.dto.openf1;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenF1SessionDto(
    @JsonProperty("session_key") int sessionKey,
    @JsonProperty("session_name") String sessionName,
    @JsonProperty("session_type") String sessionType,
    @JsonProperty("date_start") String dateStart,
    @JsonProperty("year") int year
) {}
