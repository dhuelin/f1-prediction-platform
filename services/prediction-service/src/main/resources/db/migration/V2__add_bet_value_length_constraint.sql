ALTER TABLE bonus_bets
    ADD CONSTRAINT chk_bet_value_length CHECK (char_length(bet_value) <= 100);
