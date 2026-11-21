package me.daegyeo.netflixchecker.shared.lock

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

@Component
class AdvisoryLockExecutor(
    private val dataSource: DataSource
) {
    private val logger = LoggerFactory.getLogger(AdvisoryLockExecutor::class.java)

    fun <T> runExclusively(lockKey: Long, action: () -> T): T? =
        dataSource.connection.use { connection ->
            if (!tryLock(connection, lockKey)) {
                return null
            }

            try {
                action()
            } finally {
                unlock(connection, lockKey)
            }
        }

    private fun tryLock(connection: Connection, lockKey: Long): Boolean =
        connection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { statement ->
            statement.setLong(1, lockKey)
            statement.executeQuery().use { it.next() && it.getBoolean(1) }
        }

    private fun unlock(connection: Connection, lockKey: Long) {
        runCatching {
            connection.prepareStatement("SELECT pg_advisory_unlock(?)").use { statement ->
                statement.setLong(1, lockKey)
                statement.executeQuery().close()
            }
        }.onFailure {
            logger.warn("Advisory lock 해제 중 오류가 발생했습니다. (lockKey={})", lockKey, it)
        }
    }
}
