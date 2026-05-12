package com.f1predict.f1data.dto.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1RaceControlDto(
    @JsonProperty("category") String category,
    @JsonProperty("flag") String flag,
    @JsonProperty("message") String message,
    @JsonProperty("date") String date
) {}
