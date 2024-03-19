package me.daegyeo.netflixchecker.listener

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import me.daegyeo.netflixchecker.command.Command
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CommandListener(
    private val commandRegistry: List<Command>
) : EventListener<ChatInputInteractionEvent> {
    override val eventType = ChatInputInteractionEvent::class.java

    override fun execute(event: ChatInputInteractionEvent): Mono<Void> {
        return Flux.fromIterable(commandRegistry)
            .filter { it.name == event.commandName }
            .next()
            .flatMap { it.handle(event) }
    }
}