package me.daegyeo.netflixchecker

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.rest.RestClient
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


fun main(args: Array<String>) {
    runApplication<NetflixCheckerApplication>(*args)
}

@SpringBootApplication
@ConfigurationPropertiesScan
class NetflixCheckerApplication(private val discordConfiguration: DiscordConfiguration) {

    @Bean
    fun gatewayDiscordClient(): GatewayDiscordClient? {
        return DiscordClientBuilder.create(discordConfiguration.botToken)
            .build()
            .login()
            .block()
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }
}
