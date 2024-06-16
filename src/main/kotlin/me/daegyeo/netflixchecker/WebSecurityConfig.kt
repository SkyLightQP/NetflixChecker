package me.daegyeo.netflixchecker

import me.daegyeo.netflixchecker.config.CorsOriginConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource


@Configuration
@EnableWebSecurity
class WebSecurityConfig(private val corsOriginConfiguration: CorsOriginConfiguration) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    fun corsConfigurationSource(): CorsConfigurationSource {
        return CorsConfigurationSource { _ ->
            val config = CorsConfiguration()
            config.allowedHeaders = listOf("*")
            config.allowedMethods = listOf("OPTIONS", "HEAD", "GET", "POST", "PUT", "PATCH", "DELETE")
            config.allowCredentials = true
            config.setAllowedOriginPatterns(listOf("http://localhost:3000", corsOriginConfiguration.origin))
            config
        }
    }
}