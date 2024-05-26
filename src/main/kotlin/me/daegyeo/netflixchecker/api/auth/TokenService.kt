package me.daegyeo.netflixchecker.api.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Service
class TokenService(@Value("\${jwt-secret}") private val jwtSecret: String) {

    fun generateToken(email: String): String {
        val expireDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS))

        return Jwts.builder()
            .signWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
            .subject(email)
            .expiration(expireDate)
            .issuedAt(Date.from(Instant.now()))
            .compact()!!
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
                .build()
                .parseSignedContent(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}