package me.daegyeo.netflixchecker.batch

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

@Component
class BankCrawlLock(
    private val dataSource: DataSource
) {
    companion object {
        private const val LOCK_KEY = 6_158_204_771_003L
    }

    private val logger = LoggerFactory.getLogger(BankCrawlLock::class.java)

    fun <T> runExclusively(action: () -> T): T? =
        dataSource.connection.use { connection ->
            if (!tryLock(connection)) {
                return null
            }

            try {
                action()
            } finally {
                unlock(connection)
            }
        }

    private fun tryLock(connection: Connection): Boolean =
        connection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { statement ->
            statement.setLong(1, LOCK_KEY)
            statement.executeQuery().use { it.next() && it.getBoolean(1) }
        }

    private fun unlock(connection: Connection) {
        runCatching {
            connection.prepareStatement("SELECT pg_advisory_unlock(?)").use { statement ->
                statement.setLong(1, LOCK_KEY)
                statement.executeQuery().close()
            }
        }.onFailure {
            logger.warn("은행 크롤링 잠금 해제 중 오류가 발생했습니다. 연결이 닫히면 잠금도 함께 풀립니다.", it)
        }
    }
}
