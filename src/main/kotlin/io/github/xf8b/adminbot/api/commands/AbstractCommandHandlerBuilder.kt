package io.github.xf8b.adminbot.api.commands

import discord4j.rest.util.PermissionSet
import io.github.xf8b.adminbot.api.commands.arguments.Argument
import io.github.xf8b.adminbot.api.commands.flags.Flag

//only here for java compatibility
class AbstractCommandHandlerBuilder constructor(
        name: String? = null,
        usage: String? = null,
        description: String? = null,
        commandType: AbstractCommandHandler.CommandType? = null,
        actions: MutableMap<String, String> = HashMap(),
        aliases: MutableList<String> = ArrayList(),
        minimumAmountOfArgs: Int = 0,
        flags: MutableList<Flag<*>> = ArrayList(),
        arguments: MutableList<Argument<*>> = ArrayList(),
        botRequiredPermissions: PermissionSet = PermissionSet.none(),
        administratorLevelRequired: Int = 0,
        isBotAdministratorOnly: Boolean = false,
) {
    var name: String? = name
        private set
    var usage: String? = usage
        private set
    var description: String? = description
        private set
    var commandType: AbstractCommandHandler.CommandType? = commandType
        private set
    var actions: MutableMap<String, String> = actions
        private set
    var aliases: MutableList<String> = aliases
        private set
    var minimumAmountOfArgs: Int = minimumAmountOfArgs
        private set
    var flags: MutableList<Flag<*>> = flags
        private set
    var arguments: MutableList<Argument<*>> = arguments
        private set
    var botRequiredPermissions: PermissionSet = botRequiredPermissions
        private set
    var administratorLevelRequired: Int = administratorLevelRequired
        private set
    var isBotAdministratorOnly: Boolean = isBotAdministratorOnly
        private set

    fun setName(name: String): AbstractCommandHandlerBuilder = apply {
        this.name = name
    }

    @Deprecated("Use the automatically generated usage", ReplaceWith(""))
    fun setUsage(usage: String): AbstractCommandHandlerBuilder = apply {
        this.usage = usage
    }

    fun setDescription(description: String): AbstractCommandHandlerBuilder = apply {
        this.description = description
    }

    fun setCommandType(commandType: AbstractCommandHandler.CommandType): AbstractCommandHandlerBuilder = apply {
        this.commandType = commandType
    }

    fun addAction(name: String, description: String): AbstractCommandHandlerBuilder = apply {
        actions[name] = description
    }

    fun setActions(actions: Map<String, String>): AbstractCommandHandlerBuilder = apply {
        this.actions = actions as MutableMap<String, String>
    }

    fun addAlias(alias: String): AbstractCommandHandlerBuilder = apply {
        aliases.add(alias)
    }

    fun setAliases(aliases: List<String>): AbstractCommandHandlerBuilder = apply {
        this.aliases = aliases as MutableList<String>
    }

    fun setMinimumAmountOfArgs(minimumAmountOfArgs: Int): AbstractCommandHandlerBuilder = apply {
        this.minimumAmountOfArgs = minimumAmountOfArgs
    }

    fun addFlag(flag: Flag<*>): AbstractCommandHandlerBuilder = apply {
        flags.add(flag)
    }

    fun setFlags(flags: List<Flag<*>>): AbstractCommandHandlerBuilder = apply {
        this.flags = flags as MutableList<Flag<*>>
    }

    fun addArgument(argument: Argument<*>): AbstractCommandHandlerBuilder = apply {
        arguments.add(argument)
    }

    fun setArguments(arguments: List<Argument<*>>): AbstractCommandHandlerBuilder = apply {
        this.arguments = arguments as MutableList<Argument<*>>
    }

    fun setBotRequiredPermissions(botRequiredPermissions: PermissionSet): AbstractCommandHandlerBuilder = apply {
        this.botRequiredPermissions = botRequiredPermissions
    }

    fun setAdministratorLevelRequired(administratorLevelRequired: Int): AbstractCommandHandlerBuilder = apply {
        this.administratorLevelRequired = administratorLevelRequired
    }

    fun setBotAdministratorOnly(): AbstractCommandHandlerBuilder = apply {
        isBotAdministratorOnly = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractCommandHandlerBuilder

        if (name != other.name) return false
        if (usage != other.usage) return false
        if (description != other.description) return false
        if (commandType != other.commandType) return false
        if (actions != other.actions) return false
        if (aliases != other.aliases) return false
        if (minimumAmountOfArgs != other.minimumAmountOfArgs) return false
        if (flags != other.flags) return false
        if (arguments != other.arguments) return false
        if (botRequiredPermissions != other.botRequiredPermissions) return false
        if (administratorLevelRequired != other.administratorLevelRequired) return false
        if (isBotAdministratorOnly != other.isBotAdministratorOnly) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (usage?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (commandType?.hashCode() ?: 0)
        result = 31 * result + actions.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + minimumAmountOfArgs
        result = 31 * result + flags.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + botRequiredPermissions.hashCode()
        result = 31 * result + administratorLevelRequired
        result = 31 * result + isBotAdministratorOnly.hashCode()
        return result
    }

    override fun toString(): String {
        return "AbstractCommandHandlerBuilder(name=$name, usage=$usage, description=$description, commandType=$commandType, actions=$actions, aliases=$aliases, minimumAmountOfArgs=$minimumAmountOfArgs, flags=$flags, arguments=$arguments, botRequiredPermissions=$botRequiredPermissions, administratorLevelRequired=$administratorLevelRequired, isBotAdministratorOnly=$isBotAdministratorOnly)"
    }
}