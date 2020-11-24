/*
 * Copyright (c) 2020 xf8b.
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

package io.github.xf8b.xf8bot.commands.botadministrator

import com.sun.management.OperatingSystemMXBean
import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import org.apache.commons.lang3.time.DurationFormatUtils
import reactor.core.publisher.Mono
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage

class HostInformationCommand : AbstractCommand(
    name = "\${prefix}hostinformation",
    description = "Gets information about the host.",
    commandType = CommandType.BOT_ADMINISTRATOR,
    aliases = "\${prefix}hostinfo".toSingletonImmutableList(),
    botAdministratorOnly = true
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val threadMXBean = ManagementFactory.getThreadMXBean()
        val arch = operatingSystemMXBean.arch
        val os = operatingSystemMXBean.name
        val osVersion = operatingSystemMXBean.version
        val availableProcessors = operatingSystemMXBean.availableProcessors
        val cpuLoad = operatingSystemMXBean.cpuLoad.let {
            if (it < 0) "Not available"
            else "${it * 100}%"
        }
        val freeMemory = operatingSystemMXBean.freeMemorySize / (1024 * 1024)
        val totalMemory = operatingSystemMXBean.totalMemorySize / (1024 * 1024)
        val freeSwap = operatingSystemMXBean.freeSwapSpaceSize / (1024 * 1024)
        val totalSwap = operatingSystemMXBean.totalSwapSpaceSize / (1024 * 1024)
        val uptime = DurationFormatUtils.formatDurationHMS(runtimeMXBean.uptime)
        val jvmVendor = runtimeMXBean.vmVendor
        val jvmVersion = runtimeMXBean.vmVersion
        val jvmSpecName = runtimeMXBean.specName
        val jvmSpecVendor = runtimeMXBean.specVendor
        val jvmSpecVersion = runtimeMXBean.specVersion
        val heapMemoryUsage = memoryMXBean.heapMemoryUsage
        val nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage
        val threadCount = threadMXBean.threadCount

        return event.channel.flatMap { channel ->
            channel.createEmbedDsl {
                title("Host Information")
                url("https://stackoverflow.com/a/15733233")
                description("Information about the computer that xf8bot is currently running on.")

                field("OS", os, true)
                field("OS Version", osVersion, true)
                field("Arch", arch, true)
                field("Uptime", uptime, false)
                field("Available Processors", availableProcessors.toString(), false)
                field("Memory", "${freeMemory}MB free, ${totalMemory}MB total", false)
                field("Swap", "${freeSwap}MB free, ${totalSwap}MB total", true)
                field("CPU Load", cpuLoad, false)
                field("JVM Vendor", jvmVendor, true)
                field("JVM Version", jvmVersion, true)
                field("JVM Spec", "$jvmSpecName version $jvmSpecVersion by $jvmSpecVendor", false)
                field("Thread Count", threadCount.toString(), false)
                field("Heap Memory Usage", formatMemory(heapMemoryUsage), true)
                field("Non Heap Memory Usage", formatMemory(nonHeapMemoryUsage), true)

                footer(
                    """
                    i took some of the code for this from stack overflow
                    if you are a fellow java/kotlin programmer see https://stackoverflow.com/a/15733233
                    """.trimIndent(),
                    null
                )
                timestamp()
                color(Color.BLUE)
            }
        }.then()
    }

    companion object {
        private fun formatMemory(memoryUsage: MemoryUsage): String {
            val usedMemory = bytesToMegaBytes(memoryUsage.used)
            val maxMemory = memoryUsage.max
                .takeUnless((-1L)::equals)
                ?.let { "${it / (1024 * 1024)}MB maximum" }
                ?: "no maximum"

            return "${usedMemory}MB used out of $maxMemory"
        }

        private fun bytesToMegaBytes(bytes: Long) = bytes / (1024 * 1024)
    }
}