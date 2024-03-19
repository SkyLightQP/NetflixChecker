package me.daegyeo.netflixchecker.util

import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import java.time.Instant

object EmbedUtil {
    fun create(title: String, description: String, date: Instant = Instant.now()): EmbedCreateSpec {
        return EmbedCreateSpec.builder()
            .color(Color.of(253, 121, 168))
            .title(title)
            .description(description)
            .timestamp(date)
            .build()
    }
}