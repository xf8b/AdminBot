/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.util

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

/**
 * Calculates levels for a user.
 * Adapted from [ZP4RKER's zlevels bot](https://github.com/zp4rker/zlevels/blob/master/src/main/java/me/zp4rker/zlevels/util/LevelsUtil.java).
 * @author ZP4RKER
 */
object LevelsCalculator {
    fun xpToNextLevel(level: Int): Long {
        return 5 * (level.toDouble().pow(2.0).toLong() + 10 * level + 20)
    }

    private fun levelsToXp(levels: Int): Long {
        var xp = 0L

        for (level in 0..levels) {
            xp += xpToNextLevel(level)
        }

        return xp
    }

    fun xpToLevels(totalXp: Long): Int {
        var level = 0

        while (true) {
            val xp = levelsToXp(level)

            if (totalXp < xp) {
                break
            } else {
                level++
            }
        }

        return level
    }

    fun remainingXp(totalXp: Long): Long {
        val level = xpToLevels(totalXp)
        if (level == 0) return totalXp
        val xp = levelsToXp(level)

        return totalXp - xp + xpToNextLevel(level)
    }

    fun randomXp(min: Int, max: Int): Int = ThreadLocalRandom.current().nextInt(max - min + 1) + min
}