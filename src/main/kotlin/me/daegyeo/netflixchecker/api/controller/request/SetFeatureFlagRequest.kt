package me.daegyeo.netflixchecker.api.controller.request

data class SetFeatureFlagRequest(
    val key: String,
    val value: String
)