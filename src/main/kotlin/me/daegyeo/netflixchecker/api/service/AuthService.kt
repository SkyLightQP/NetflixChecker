package me.daegyeo.netflixchecker.api.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import me.daegyeo.netflixchecker.api.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthService(private val supabaseClient: SupabaseClient) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun login(emailInput: String, passwordInput: String): String {
        try {
            supabaseClient.auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }

            val session = supabaseClient.auth.currentSessionOrNull() ?: throw ServiceException("세션을 찾을 수 없습니다.", 404)

            logger.info("${emailInput} 관리자가 로그인 하였습니다.")

            return session.user!!.email!!
        } catch (e: BadRequestRestException) {
            if (e.error == "invalid_grant") {
                throw ServiceException("이메일 또는 비밀번호가 올바르지 않습니다.", 401)
            }
            throw e
        }
    }

    suspend fun logout() {
        supabaseClient.auth.signOut()
    }
}