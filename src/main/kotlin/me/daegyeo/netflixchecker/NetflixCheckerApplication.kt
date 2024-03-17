package me.daegyeo.netflixchecker

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.rest.RestClient
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import me.daegyeo.netflixchecker.listener.EventListener
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


fun main(args: Array<String>) {
    runApplication<NetflixCheckerApplication>(*args)
}

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportAutoConfiguration(
    value = [ExposedAutoConfiguration::class],
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class NetflixCheckerApplication(private val discordConfiguration: DiscordConfiguration) {

    @Bean
    fun <T : Event> gatewayDiscordClient(eventListeners: List<EventListener<T>>): GatewayDiscordClient {
        val client = DiscordClientBuilder.create(discordConfiguration.botToken)
            .build()
            .login()
            .block()

        for (listener in eventListeners) {
            client!!.on(listener.eventType)
                .flatMap(listener::execute)
                .onErrorResume(listener::handleError)
                .subscribe()
        }

        return client!!
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }
}
