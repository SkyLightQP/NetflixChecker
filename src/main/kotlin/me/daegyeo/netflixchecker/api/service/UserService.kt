package me.daegyeo.netflixchecker.api.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import me.daegyeo.netflixchecker.api.exception.ServiceException
import org.springframework.stereotype.Service

@Service
class UserService(private val supabaseClient: SupabaseClient) {

    suspend fun getCurrentUserEmail(): String {
        val session = supabaseClient.auth.currentSessionOrNull() ?: throw ServiceException("세션을 찾을 수 없습니다.", 404)
        return session.user!!.email!!
    }
}