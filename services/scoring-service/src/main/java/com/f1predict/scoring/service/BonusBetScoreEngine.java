package com.f1predict.scoring.service;

import com.f1predict.scoring.dto.BetData;
import com.f1predict.scoring.dto.RaceResultData;
import com.f1predict.scoring.dto.ScoringConfigData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure scoring engine for bonus bets.
 *
 * Win condition per bet type:
 *   FASTEST_LAP  — betValue driver set the fastest lap
 *   DNF_DSQ_DNS  — betValue driver appears in dnfDsqDnsDrivers
 *   SC_DEPLOYED  — betValue "true" if safetyCarsDeployed > 0, "false" otherwise
 *   SC_COUNT     — betValue matches exact number of safety cars deployed
 *
 * Win payout  = +floor(stake * multiplier)
 * Lose penalty = -stake
 */
@Component
public class BonusBetScoreEngine {

    public int scoreBets(List<BetData> bets, RaceResultData result, ScoringConfigData config) {
        if (bets == null || bets.isEmpty()) return 0;
        int total = 0;
        for (BetData bet : bets) {
            if (!isBetTypeEnabled(bet.betType(), config)) continue;
            boolean won = evaluate(bet, result);
            if (won) {
                total += (int) config.betMultiplier()
                    .multiply(BigDecimal.valueOf(bet.stake()))
                    .setScale(0, RoundingMode.FLOOR)
                    .intValue();
            } else {
                total -= bet.stake();
            }
        }
        return total;
    }

    private boolean evaluate(BetData bet, RaceResultData result) {
        return switch (bet.betType()) {
            case "FASTEST_LAP" -> bet.betValue() != null && bet.betValue().equals(result.fastestLapDriver());
            case "DNF_DSQ_DNS" -> result.dnfDsqDnsDrivers() != null
                && result.dnfDsqDnsDrivers().contains(bet.betValue());
            case "SC_DEPLOYED" -> {
                boolean scHappened = result.safetyCarsDeployed() > 0;
                yield "true".equalsIgnoreCase(bet.betValue()) == scHappened;
            }
            case "SC_COUNT" -> {
                try {
                    yield Integer.parseInt(bet.betValue()) == result.safetyCarsDeployed();
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }

    private boolean isBetTypeEnabled(String betType, ScoringConfigData config) {
        return switch (betType) {
            case "FASTEST_LAP" -> config.fastestLapEnabled();
            case "DNF_DSQ_DNS" -> config.dnfDsqDnsEnabled();
            case "SC_DEPLOYED" -> config.scDeployedEnabled();
            case "SC_COUNT"    -> config.scCountEnabled();
            default -> false;
        };
    }
}
