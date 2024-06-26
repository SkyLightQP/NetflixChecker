package me.daegyeo.netflixchecker

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.rest.RestClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.JacksonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import me.daegyeo.netflixchecker.config.SupabaseConfiguration
import me.daegyeo.netflixchecker.listener.EventListener
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling


fun main(args: Array<String>) {
    runApplication<NetflixCheckerApplication>(*args)
}

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportAutoConfiguration(
    value = [ExposedAutoConfiguration::class],
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
@EnableScheduling
class NetflixCheckerApplication(
    private val discordConfiguration: DiscordConfiguration,
    private val supabaseConfiguration: SupabaseConfiguration
) {
    private val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    @Bean
    fun supabase(): SupabaseClient {
        val supabase = createSupabaseClient(
            supabaseUrl = supabaseConfiguration.url,
            supabaseKey = supabaseConfiguration.key
        ) {
            defaultSerializer = JacksonSerializer()
            install(Auth)
            install(Postgrest)
        }

        return supabase
    }

    @Bean
    fun coroutineScope(): CoroutineScope = applicationCoroutineScope
}
