package me.daegyeo.netflixchecker.alert

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import me.daegyeo.netflixchecker.config.FeatureFlagConfiguration
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import me.daegyeo.netflixchecker.event.OccurredCrawlErrorEvent
import me.daegyeo.netflixchecker.shared.util.EmbedUtil
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Component
class ErrorAlert(
    private val gatewayDiscordClient: GatewayDiscordClient,
    private val featureFlagConfiguration: FeatureFlagConfiguration
) {
    @EventListener
    fun sendErrorAlert(event: OccurredCrawlErrorEvent) {
        if ((event.type == "BANK" && !featureFlagConfiguration.getBoolean(FeatureFlagKey.ENABLE_BANK_CRAWLING_ERROR_ALERT)) ||
            (event.type == "CODE" && !featureFlagConfiguration.getBoolean(FeatureFlagKey.ENABLE_CODE_CRAWLING_ERROR_ALERT))
        ) {
            return
        }

        val channel =
            gatewayDiscordClient.getChannelById(Snowflake.of(featureFlagConfiguration.getString(FeatureFlagKey.DISCORD_CHANNEL_ID)))
                .ofType(GuildMessageChannel::class.java).block()!!

        val embed = EmbedUtil.create(
            title = "크롤링 오류",
            description = "${event.type} 크롤링 오류가 발생했습니다.\n```${event.message}```",
        )
        channel.createMessage(embed).block()
    }
}