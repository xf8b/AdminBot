/*
 * Taken from https://github.com/zp4rker/zlevels/blob/master/src/main/java/me/zp4rker/zlevels/util/LevelsUtil.java
 */
package io.github.xf8b.adminbot.util;

import java.util.Random;

public class LevelUtil {
    public static long xpToNextLevel(int level) {
        return 10 * (((long) Math.pow(level, 2)) + 10 * level + 20);
    }

    private static long levelsToXp(int levels) {
        long xp = 0;

        for (int level = 0; level <= levels; level++) {
            xp += xpToNextLevel(level);
        }

        return xp;
    }

    public static int xpToLevels(long totalXp) {
        boolean calculating = true;
        int level = 0;

        while (calculating) {
            long xp = levelsToXp(level);

            if (totalXp < xp) {
                calculating = false;
            } else {
                level++;
            }
        }

        return level;
    }

    public static long remainingXp(long totalXp) {
        int level = xpToLevels(totalXp);

        if (level == 0) return totalXp;

        long xp = levelsToXp(level);

        return totalXp - xp + xpToNextLevel(level);
    }

    public static int randomXp(int min, int max) {
        Random random = new Random();

        return random.nextInt((max - min) + 1) + min;
    }
}
