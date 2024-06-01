package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.controller.dto.AuthLoginRequest
import me.daegyeo.netflixchecker.api.service.AuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AuthLoginRequest): Map<String, String> {
        val email = authService.login(request.email, request.password)
        return mapOf("email" to email)
    }

    @DeleteMapping("/logout")
    suspend fun logout() {
        authService.logout()
    }
}
