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

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.Command.ExecutionChecks
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.bool.logicalAnd
import java.util.function.Predicate

object Checks {
    fun isClientExceptionWithCode(code: Int): Predicate<Throwable> = Predicate { throwable ->
        if (throwable is ClientException) {
            throwable.errorResponse
                .map { it.fields["code"] }
                .map { it as Int == code }
                .orElse(false)
        } else {
            false
        }
    }

    /**
     * Checks if the member from [event] is higher than [member]. If they are not, sends an error message.
     */
    fun isMemberHighEnough(event: CommandFiredEvent, member: Member, action: String): Mono<Boolean> =
        event.guild.flatMap { guild ->
            PermissionUtil.isMemberHigherOrEqual(
                event.xf8bot,
                guild,
                firstMember = event.member.get(), secondMember = member
            ).flatMap { higher ->
                if (!higher) {
                    event.channel
                        .flatMap { it.createMessage("Cannot $action member because the member is higher than or equal to you!") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            }
        }

    /**
     * Checks if the member from [event] can use administrative actions (e.g. ban, kick) on [member]. If they cannot, sends an error message.
     */
    fun canMemberUseAdministrativeActionsOn(event: CommandFiredEvent, member: Member, action: String) =
        Mono.just(event.member.get() == member)
            .flatMap { usedOnSelf ->
                if (usedOnSelf) {
                    event.channel
                        .flatMap { it.createMessage("You cannot $action yourself!") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            }
            .logicalAnd(event.guild.flatMap { it.selfMember.map(member::equals) }.flatMap { usedOnXf8bot ->
                if (usedOnXf8bot) {
                    event.channel
                        .flatMap { it.createMessage("You cannot $action xf8bot!") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            })

    /**
     * Checks if the bot (self member) can interact with (e.g. ban, kick. nickname) [member]. If the bot cannot, sends error message.
     *
     * This uses [Member.isHigher].
     */
    fun canBotInteractWith(
        guild: Guild,
        member: Member,
        channel: Publisher<MessageChannel>,
        action: String
    ): Mono<Boolean> =
        guild.selfMember.flatMap(member::isHigher)
            .flatMap { higher ->
                if (/* if bot is */!higher) {
                    channel.toMono()
                        .flatMap { it.createMessage("Cannot $action member because the member is higher than me!") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            }

    fun doesBotHavePermissionsRequired(
        command: Command,
        bot: Publisher<Member>,
        channel: Publisher<MessageChannel>
    ): Mono<Boolean> =
        if (ExecutionChecks.BOT_HAS_REQUIRED_PERMISSIONS in command.disabledChecks) {
            true.toMono()
        } else {
            bot.toMono().flatMap(Member::getBasePermissions)
                .map { it.contains(Permission.ADMINISTRATOR) || it.containsAll(command.botRequiredPermissions) }
                .flatMap { permissionsMet ->
                    if (!permissionsMet) {
                        channel.toMono()
                            .flatMap { it.createMessage("Could not execute command \'${command.rawName}\' because of insufficient permissions!") }
                            .thenReturn(false)
                    } else {
                        true.toMono()
                    }
                }
        }

    fun doesMemberHaveCorrectAdministratorLevel(command: Command, event: CommandFiredEvent): Mono<Boolean> =
        if (ExecutionChecks.IS_ADMINISTRATOR in command.disabledChecks) {
            true.toMono()
        } else {
            event.guild
                .flatMap { PermissionUtil.canMemberUseCommand(event.xf8bot, it, event.member.get(), command) }
                .flatMap { allowed ->
                    if (!allowed) {
                        event.channel
                            .flatMap { it.createMessage("Sorry, you don't have high enough permissions.") }
                            .thenReturn(false)
                    } else {
                        true.toMono()
                    }
                }
        }

    fun canMemberUseBotAdministratorOnlyCommand(
        command: Command,
        xf8bot: Xf8bot,
        member: Member,
        channel: Publisher<MessageChannel>
    ): Mono<Boolean> = when {
        ExecutionChecks.IS_BOT_ADMINISTRATOR in command.disabledChecks -> true.toMono() // ignore if check is disabled

        command.botAdministratorOnly && !xf8bot.isBotAdministrator(member.id) -> channel.toMono()
            .flatMap { it.createMessage("Sorry, you aren't an administrator of xf8bot.") }
            .thenReturn(false)

        else -> true.toMono()
    }

    fun isThereEnoughArguments(command: Command, event: CommandFiredEvent): Mono<Boolean> = when {
        ExecutionChecks.SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS in command.disabledChecks -> true.toMono()

        event.message.content.split(" ").drop(1).size < command.minimumAmountOfArgs -> {
            command.getUsageWithPrefix(event.xf8bot, event.guildId.get().asString()).flatMap { usage ->
                event.channel
                    .flatMap { it.createMessage("Huh? Could you repeat that? The usage of this command is: `$usage`.") }
                    .thenReturn(false)
            }
        }

        else -> true.toMono()
    }
}
