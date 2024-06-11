package me.daegyeo.netflixchecker.api.service

import me.daegyeo.netflixchecker.entity.DepositLog
import me.daegyeo.netflixchecker.entity.Metric
import me.daegyeo.netflixchecker.enum.MetricsKey
import me.daegyeo.netflixchecker.table.DepositLogs
import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.month
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId


@Service
class MetricsService {

    fun getThisMonthDepositCount(): Int {
        val now = Instant.now().atZone(ZoneId.of("Asia/Seoul"))
        val month = now.monthValue

        val count = transaction {
            DepositLog.find {
                DepositLogs.date.month() eq month
            }.count()
        }

        return count.toInt()
    }

    fun getLatestDepositor(): String {
        val now = Instant.now().atZone(ZoneId.of("Asia/Seoul"))
        val month = now.monthValue

        val query = transaction {
            DepositLog.find {
                DepositLogs.date.month() eq month
            }.orderBy(DepositLogs.date to SortOrder.DESC).limit(1).firstOrNull()
        }
        val dec = DecimalFormat("#,###")

        if (query == null) return "입금 내역이 없습니다."

        return "${query.who} (${dec.format(query.cost)} 원)"
    }

    fun getCodeGeneratedCount(): Int {
        val count = transaction {
            Metric.find { Metrics.key eq MetricsKey.CODE_GENERATED_COUNT.key }.firstOrNull()
        } ?: return 0
        return count.value.toInt()
    }

    fun getIsSuccessLatestCrawling(): Boolean {
        val data = transaction {
            Metric.find { Metrics.key eq MetricsKey.LATEST_CRAWLING_STATUS.key }.firstOrNull()
        } ?: return false
        return data.value == "O"
    }

    fun getLatestCrawlingTime(): String {
        val data = transaction {
            Metric.find { Metrics.key eq MetricsKey.LATEST_CRAWLING_TIME.key }.firstOrNull()
        } ?: return "정보 없음"
        return data.value
    }
}