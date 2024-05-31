package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.controller.dto.AuthLoginRequest
import me.daegyeo.netflixchecker.api.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AuthLoginRequest): String {
        return authService.login(request.email, request.password)
    }

    @DeleteMapping("/logout")
    suspend fun login(): ResponseStatus {
        authService.logout()
        return ResponseStatus(HttpStatus.OK)
    }
}
