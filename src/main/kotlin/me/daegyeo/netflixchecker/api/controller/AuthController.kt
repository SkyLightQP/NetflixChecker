package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.controller.request.AuthLoginRequest
import me.daegyeo.netflixchecker.api.controller.request.PublicLoginRequest
import me.daegyeo.netflixchecker.api.service.AuthService
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AuthLoginRequest): Map<String, String> {
        val email = authService.login(request.email, request.password)
        return mapOf("email" to email)
    }

    @CheckAuth
    @DeleteMapping("/logout")
    suspend fun logout() {
        authService.logout()
    }

    @PostMapping("/public/login")
    fun publicLogin(@RequestBody request: PublicLoginRequest): Map<String, String> {
        return mapOf("token" to authService.verifyPublicApiPassword(request.password))
    }
}
