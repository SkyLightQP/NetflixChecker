package me.daegyeo.netflixchecker.api.controller

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.daegyeo.netflixchecker.api.exception.ServiceException
import me.daegyeo.netflixchecker.api.service.PublicService
import me.daegyeo.netflixchecker.config.PublicApiConfiguration
import me.daegyeo.netflixchecker.crawler.VerificationCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public")
class PublicController(
    private val publicService: PublicService,
    private val publicApiConfiguration: PublicApiConfiguration
) {

    @GetMapping("/code")
    fun getCode(@RequestHeader("Authorization") authorization: String): VerificationCode {
        try {
            val token = authorization.substringAfter("Bearer ", "").trim()

            if (token.isEmpty()) {
                throw ServiceException("권한이 없습니다.", 401)
            }

            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(publicApiConfiguration.secret.toByteArray())).build()
                .parseSignedClaims(token)

            return publicService.getCode()
        } catch (e: JwtException) {
            throw ServiceException("권한이 없습니다.", 401)
        }
    }
}
