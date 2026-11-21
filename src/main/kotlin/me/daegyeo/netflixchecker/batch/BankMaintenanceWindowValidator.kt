package me.daegyeo.netflixchecker.batch

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.JobParametersValidator
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class BankMaintenanceWindowValidator : JobParametersValidator {
    override fun validate(parameters: JobParameters?) {
        val requestedAt = parameters?.getLocalDateTime(BankCrawlJobConfiguration.REQUESTED_AT)
            ?: throw JobParametersInvalidException("${BankCrawlJobConfiguration.REQUESTED_AT} 파라미터가 필요합니다.")

        if (isMaintenanceTime(requestedAt.toLocalTime())) {
            throw JobParametersInvalidException("은행 점검 시간입니다. (23:30 ~ 01:00)")
        }
    }

    private fun isMaintenanceTime(time: LocalTime): Boolean =
        !time.isBefore(MAINTENANCE_START) || time.isBefore(MAINTENANCE_END)

    companion object {
        private val MAINTENANCE_START = LocalTime.of(23, 30)
        private val MAINTENANCE_END = LocalTime.of(1, 0)
    }
}
