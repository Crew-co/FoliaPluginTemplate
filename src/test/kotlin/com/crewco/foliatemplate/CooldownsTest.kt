package com.crewco.foliatemplate

import com.crewco.foliatemplate.util.Cooldowns
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class CooldownsTest {

    @Test
    fun `first test passes, then blocks until it expires`() {
        var now = 0L
        val cooldowns = Cooldowns(nanoClock = { now })
        val id = UUID.randomUUID()

        assertTrue(cooldowns.test(id, Duration.ofSeconds(3)), "first use should be allowed")
        assertFalse(cooldowns.test(id, Duration.ofSeconds(3)), "second use should be blocked")

        now += Duration.ofSeconds(3).toNanos()
        assertTrue(cooldowns.test(id, Duration.ofSeconds(3)), "should be allowed again after expiry")
    }

    @Test
    fun `remainingSeconds rounds up`() {
        var now = 0L
        val cooldowns = Cooldowns(nanoClock = { now })
        val id = UUID.randomUUID()

        cooldowns.test(id, Duration.ofSeconds(3))
        now += Duration.ofMillis(1500).toNanos()

        assertEquals(2L, cooldowns.remainingSeconds(id), "1.5s left should round up to 2")
    }

    @Test
    fun `independent keys do not interfere`() {
        val cooldowns = Cooldowns(nanoClock = { 0L })
        assertTrue(cooldowns.test(UUID.randomUUID(), Duration.ofSeconds(5)))
        assertTrue(cooldowns.test(UUID.randomUUID(), Duration.ofSeconds(5)))
    }
}
