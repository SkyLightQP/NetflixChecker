package me.daegyeo.netflixchecker.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import reactor.core.publisher.Mono



interface Command {
    val name: String

    val description: String

    val options: List<CommandOption>

    fun handle(event: ChatInputInteractionEvent): Mono<Void>
}

data class CommandOption(val name: String, val description: String, val type: Int, val required: Boolean)