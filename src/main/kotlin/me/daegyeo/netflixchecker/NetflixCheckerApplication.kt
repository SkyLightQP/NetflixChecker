package me.daegyeo.netflixchecker

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.rest.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

fun main(args: Array<String>) {
    runApplication<NetflixCheckerApplication>(*args)
}

@SpringBootApplication
class NetflixCheckerApplication {
    @Value("\${token}")
    private lateinit var token: String

    @Bean
    fun gatewayDiscordClient(): GatewayDiscordClient? {
        return DiscordClientBuilder.create(token)
            .build()
            .login()
            .block()
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }
}
