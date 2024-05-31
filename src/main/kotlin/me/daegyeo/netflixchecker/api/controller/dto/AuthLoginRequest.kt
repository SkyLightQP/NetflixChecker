package me.daegyeo.netflixchecker.api.controller.dto

data class AuthLoginRequest(
    val email: String,
    val password: String
)
