package fr.bananasmoothii.snowwars;

/**
 * Used to check the snowballs throwing rate.
 */
public class Snowballs {
    private int snowballStrike;
    private long lastSnowball;
    private long[] snowballs = new long[Config.AntiCheat.snowballCheck];
    private int index;
    private long lastPunition;

    /**
     * Acts like a setter and a getter: it saves the snowball timestamp and
     * @return {@code true} if there is a problem with the rate and the player should be punished.
     *         If it returns true, all latest snowball timestamps are reset.
     */
    public boolean snowballThrownTooFast() {
        long time = System.currentTimeMillis();
        if (time - lastPunition < Config.AntiCheat.punitionCooldown) return false;
        if (time - lastSnowball >= Config.AntiCheat.maxSnowballAge * 1000L) {
            reset();
            lastSnowball = time;
            snowballStrike = 1;
            addSnowball(time);
            return false;
        }
        lastSnowball = time;
        snowballStrike++;
        addSnowball(time);
        if (snowballStrike >= Config.AntiCheat.snowballCheck) {
            if (lastSnowball - getSnowballStrikeStart() <= Config.AntiCheat.minSnowballInterval * Config.AntiCheat.snowballCheck * 1000) {
                reset();
                return true;
            }
        }
        return false;
    }

    private void addSnowball(long time) {
        snowballs[index++] = time;
        if (index == snowballs.length) index = 0;
    }

    public long getSnowballStrikeStart() {
        return snowballs[index];
    }

    public void reset() {
        snowballStrike = 0;
        snowballs = new long[Config.AntiCheat.snowballCheck];
        index = 0;
    }
}
