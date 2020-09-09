package io.github.xf8b.adminbot.api.commands.flags

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.concurrent.TimeUnit


class FlagTest {
    @Test
    fun `test flag regex`() {
        val flagRegex = Flag.REGEX.toRegex()
        assertTrue(flagRegex.matches("-c blue"))
        assertTrue(flagRegex.matches("-f \"beans\""))
        assertTrue(flagRegex.matches("-m = \"bruhman\""))
        assertTrue(flagRegex.matches("--red=true"))
    }
}

class IntegerFlagTest {
    @Test
    fun `test integer flag validity check`() {
        val integerFlag = IntegerFlag.builder()
                .setShortName("i")
                .setLongName("integer")
                .build();
        assertFalse(integerFlag.isValidValue("beans"))
        assertFalse(integerFlag.isValidValue("\"2\""))
    }
}

class TimeFlagTest {
    @Test
    fun `test time flag parse result`() {
        val timeFlag = TimeFlag.builder()
                .setShortName("t")
                .setLongName("time")
                .build()
        assertTrue {
            timeFlag.parse("2d").left == 2L
        }
        assertTrue {
            timeFlag.parse("2min").right == TimeUnit.MINUTES
        }
    }
}