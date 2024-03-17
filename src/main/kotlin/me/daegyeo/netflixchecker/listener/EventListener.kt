package me.daegyeo.netflixchecker.listener

import discord4j.core.event.domain.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

interface EventListener<T : Event> {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(EventListener::class.java)
    }

    val eventType: Class<T>

    fun execute(event: T): Mono<Void>

    fun handleError(error: Throwable?): Mono<Void> {
        logger.error("봇 이벤트 처리 중 오류가 발생했습니다: " + eventType.simpleName, error)
        return Mono.empty()
    }

}