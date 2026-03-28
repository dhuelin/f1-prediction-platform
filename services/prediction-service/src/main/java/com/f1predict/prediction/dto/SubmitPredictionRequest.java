package com.f1predict.prediction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitPredictionRequest(
    @NotEmpty @Size(min = 1, max = 20) List<String> rankedDriverCodes,
    @Pattern(regexp = "RACE|SPRINT") String sessionType  // defaults to "RACE" if null
) {}
