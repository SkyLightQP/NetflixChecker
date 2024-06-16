package me.daegyeo.netflixchecker.api.controller.request

data class AuthLoginRequest(
    val email: String,
    val password: String
)
