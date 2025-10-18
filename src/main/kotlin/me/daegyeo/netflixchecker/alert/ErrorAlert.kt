package me.daegyeo.netflixchecker.alert

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import me.daegyeo.netflixchecker.config.DiscordConfiguration
import me.daegyeo.netflixchecker.event.OccurredCrawlErrorEvent
import me.daegyeo.netflixchecker.shared.util.EmbedUtil
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Component
class ErrorAlert(
    private val gatewayDiscordClient: GatewayDiscordClient, private val discordConfiguration: DiscordConfiguration
) {
    @EventListener
    fun sendErrorAlert(event: OccurredCrawlErrorEvent) {
        val channel = gatewayDiscordClient.getChannelById(Snowflake.of(discordConfiguration.channel))
            .ofType(GuildMessageChannel::class.java).block()!!

        val embed = EmbedUtil.create(
            title = "크롤링 오류",
            description = "${event.type} 크롤링 오류가 발생했습니다.\n```${event.message}```",
        )
        channel.createMessage(embed).block()
    }
}