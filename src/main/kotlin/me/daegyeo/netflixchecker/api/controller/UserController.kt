package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.service.UserService
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(private val userService: UserService) {

    @CheckAuth
    @GetMapping("/me")
    suspend fun me(): Map<String, String> {
        return mapOf("email" to userService.getCurrentUserEmail())
    }
}