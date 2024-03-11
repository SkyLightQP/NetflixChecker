package me.daegyeo.netflixchecker.event

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import me.daegyeo.netflixchecker.command.ICommand
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CommandListener(
    private val commandRegistry: List<ICommand>
) : EventListener<ChatInputInteractionEvent> {
    override val eventType = ChatInputInteractionEvent::class.java

    override fun execute(event: ChatInputInteractionEvent): Mono<Void> {
        return Flux.fromIterable(commandRegistry)
            .filter { it.getName() == event.commandName }
            .next()
            .flatMap { it.handle(event) }
    }
}