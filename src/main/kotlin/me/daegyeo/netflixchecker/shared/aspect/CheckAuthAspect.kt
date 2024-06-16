package me.daegyeo.netflixchecker.shared.aspect

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import me.daegyeo.netflixchecker.api.exception.ServiceException
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class CheckAuthAspect(private val supabaseClient: SupabaseClient) {

    @Before(value = "@annotation(checkAuth)")
    fun checkAuth(checkAuth: CheckAuth) {
        supabaseClient.auth.currentSessionOrNull() ?: throw ServiceException("권한이 없습니다.", 401)
    }
}