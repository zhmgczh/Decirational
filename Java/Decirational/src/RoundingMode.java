package decirational;

/** How {@link Rational#toRoundDecimalStringMode} resolves an exact tie (a discarded fraction of precisely 1/2). */
public enum RoundingMode {
    /** Rounds an exact tie away from zero. This is the mode {@link Rational#toRoundDecimalString} has always used. */
    HALF_UP,
    /**
     * Rounds an exact tie to whichever neighbor has an even last digit ("banker's rounding"), the convention many
     * accounting systems use to avoid biasing sums of rounded values upward.
     */
    HALF_EVEN
}
