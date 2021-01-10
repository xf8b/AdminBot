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
        val operatingSystemMxBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val memoryMxBean = ManagementFactory.getMemoryMXBean()
        val threadMxBean = ManagementFactory.getThreadMXBean()
        val arch = operatingSystemMxBean.arch
        val os = operatingSystemMxBean.name
        val osVersion = operatingSystemMxBean.version
        val availableProcessors = operatingSystemMxBean.availableProcessors
        val cpuLoad = operatingSystemMxBean.cpuLoad.let { if (it < 0.0) "Not available" else "${it * 100.0}%" }
        val freeMemory = operatingSystemMxBean.freeMemorySize.bytesToMegaBytes()
        val totalMemory = operatingSystemMxBean.totalMemorySize.bytesToMegaBytes()
        val freeSwap = operatingSystemMxBean.freeSwapSpaceSize.bytesToMegaBytes()
        val totalSwap = operatingSystemMxBean.totalSwapSpaceSize.bytesToMegaBytes()
        val uptime = DurationFormatUtils.formatDurationHMS(runtimeMxBean.uptime)
        val jvmVendor = runtimeMxBean.vmVendor
        val jvmVersion = runtimeMxBean.vmVersion
        val jvmSpecName = runtimeMxBean.specName
        val jvmSpecVendor = runtimeMxBean.specVendor
        val jvmSpecVersion = runtimeMxBean.specVersion
        val heapMemoryUsage = memoryMxBean.heapMemoryUsage
        val nonHeapMemoryUsage = memoryMxBean.nonHeapMemoryUsage
        val threadCount = threadMxBean.threadCount

        return event.channel.flatMap { channel ->
            channel.createEmbedDsl {
                title("Host Information")
                url("https://stackoverflow.com/a/15733233")
                description("Information about the computer that xf8bot is currently running on.")

                field("OS", os, inline = true)
                field("OS Version", osVersion, inline = true)
                field("Arch", arch, inline = true)
                field("Uptime", uptime, inline = false)
                field("Available Processors", availableProcessors.toString(), inline = false)
                field("Memory", "${freeMemory}MB free, ${totalMemory}MB total", inline = true)
                field("Swap", "${freeSwap}MB free, ${totalSwap}MB total", inline = true)
                field("CPU Load", cpuLoad, inline = false)
                field("JVM Vendor", jvmVendor, inline = true)
                field("JVM Version", jvmVersion, inline = true)
                field("JVM Spec", "$jvmSpecName version $jvmSpecVersion by $jvmSpecVendor", inline = false)
                field("Thread Count", threadCount.toString(), inline = false)
                field("Heap Memory Usage", heapMemoryUsage.formatted(), inline = true)
                field("Non Heap Memory Usage", nonHeapMemoryUsage.formatted(), inline = true)

                footer(
                    """
                    i took some of the code for this from stack overflow
                    if you are a fellow java/kotlin programmer see https://stackoverflow.com/a/15733233
                    """.trimIndent()
                )
                timestamp()
                color(Color.BLUE)
            }
        }.then()
    }

    companion object {
        private fun MemoryUsage.formatted(): String {
            val usedMemory = this.used.bytesToMegaBytes()
            val maxMemory = if (this.max == -1L) "no maximum" else "${this.max.bytesToMegaBytes()}MB maximum"

            return "${usedMemory}MB used out of $maxMemory"
        }

        private fun Long.bytesToMegaBytes() = this / (1000 * 1000)
    }
}