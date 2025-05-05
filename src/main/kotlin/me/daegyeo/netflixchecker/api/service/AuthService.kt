package me.daegyeo.netflixchecker.api.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.daegyeo.netflixchecker.api.exception.ServiceException
import me.daegyeo.netflixchecker.config.PublicApiConfiguration
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

@Service
class AuthService(
    private val supabaseClient: SupabaseClient,
    private val passwordEncoder: PasswordEncoder,
    private val publicApiConfiguration: PublicApiConfiguration
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun login(emailInput: String, passwordInput: String): String {
        return try {
            supabaseClient.auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }

            val session = supabaseClient.auth.currentSessionOrNull() ?: throw ServiceException("세션을 찾을 수 없습니다.", 404)

            logger.info("$emailInput 관리자가 로그인 하였습니다.")

            session.user!!.email!!
        } catch (e: BadRequestRestException) {
            if (e.error == "Invalid login credentials") {
                throw ServiceException("이메일 또는 비밀번호가 올바르지 않습니다.", 401)
            }
            throw e
        }
    }

    suspend fun logout() {
        try {
            val targetEmail =
                supabaseClient.auth.currentSessionOrNull()?.user?.email ?: throw ServiceException("세션을 찾을 수 없습니다.", 404)
            supabaseClient.auth.clearSession()
            supabaseClient.auth.signOut()
            logger.info("$targetEmail 관리자가 로그아웃 하였습니다.")
        } catch (e: UnknownRestException) {
            return
        }
    }

    fun verifyPublicApiPassword(inputPassword: String): String {
        val TWO_MINUTES = 120000L

        try {
            val result = passwordEncoder.matches(inputPassword, publicApiConfiguration.password)
            if (!result) {
                logger.warn("인증코드용 공개 API 접근 비밀번호가 일치하지 않습니다.")
                throw ServiceException("비밀번호가 올바르지 않습니다.", 401)
            }

            val currentTime = Instant.now().toEpochMilli()
            val token =
                Jwts.builder()
                    .subject("public-api")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date(currentTime + TWO_MINUTES))
                    .signWith(Keys.hmacShaKeyFor(publicApiConfiguration.secret.toByteArray()))
                    .compact()

            logger.info("인증코드용 공개 API 접근 비밀번호를 검증했습니다.")

            return token
        } catch (e: Exception) {
            logger.error("인증코드용 공개 API 접근 비밀번호 검증 중 오류가 발생했습니다.", e)
            throw ServiceException("인증코드용 공개 API 접근 비밀번호 검증 중 오류가 발생했습니다.", 500)
        }
    }
}