package me.daegyeo.netflixchecker.shared.util

import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import java.time.Instant

object EmbedUtil {
    fun create(title: String, description: String, date: Instant = Instant.now()): EmbedCreateSpec {
        return EmbedCreateSpec.builder()
            .color(Color.of(191, 9, 47))
            .title(title)
            .description(description)
            .timestamp(date)
            .build()
    }
}