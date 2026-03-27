package com.f1predict.scoring.dto;

import java.util.List;

/**
 * Parsed race result for scoring.
 *
 * @param classifiedFinishers ordered list of finishers (position 1 = index 0)
 * @param fastestLapDriver    driver code who set the fastest lap (null if not applicable)
 * @param dnfDsqDnsDrivers    drivers who DNF'd, were DSQ'd, or DNS'd
 * @param safetyCarsDeployed  number of safety cars deployed (0 = none)
 * @param partialDistance     true if race ended before 75% distance
 * @param cancelled           true if race was cancelled entirely
 */
public record RaceResultData(
    List<DriverResult> classifiedFinishers,
    String fastestLapDriver,
    List<String> dnfDsqDnsDrivers,
    int safetyCarsDeployed,
    boolean partialDistance,
    boolean cancelled
) {}
