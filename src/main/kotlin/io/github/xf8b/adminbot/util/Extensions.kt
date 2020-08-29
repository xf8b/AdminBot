package io.github.xf8b.adminbot.util

import discord4j.core.`object`.entity.Member

//member extensions
fun Member.getTagWithDisplayName(): String = this.displayName + "#" + this.discriminator
