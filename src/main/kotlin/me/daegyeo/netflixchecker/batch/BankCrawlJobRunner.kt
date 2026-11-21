package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.event.OccurredCrawlErrorEvent
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.repository.JobRestartException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BankCrawlTrigger { SCHEDULED, DISCORD, API }

sealed interface BankCrawlRunResult {
    data class Completed(val depositCount: Int) : BankCrawlRunResult
    data object Failed : BankCrawlRunResult
    data object Maintenance : BankCrawlRunResult
    data object AlreadyRunning : BankCrawlRunResult
    data object DuplicateRun : BankCrawlRunResult
}

@Component
class BankCrawlJobRunner(
    private val jobLauncher: JobLauncher,
    private val bankCrawlJob: Job,
    private val jobExplorer: JobExplorer,
    private val jobRepository: JobRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(BankCrawlJobRunner::class.java)

    fun run(trigger: BankCrawlTrigger): BankCrawlRunResult =
        try {
            launchUnlessRunning(trigger)
        } catch (e: JobParametersInvalidException) {
            logger.info("은행 크롤링 Job을 시작하지 않습니다. (trigger={}, reason={})", trigger, e.message)
            BankCrawlRunResult.Maintenance
        } catch (e: JobExecutionAlreadyRunningException) {
            logger.info("같은 회차의 은행 크롤링 Job이 이미 실행 중입니다. (trigger={})", trigger)
            BankCrawlRunResult.AlreadyRunning
        } catch (e: JobInstanceAlreadyCompleteException) {
            logger.info("같은 회차의 은행 크롤링 Job이 이미 실행되었습니다. (trigger={})", trigger)
            BankCrawlRunResult.DuplicateRun
        } catch (e: JobRestartException) {
            logger.info("같은 회차의 은행 크롤링 Job이 이미 실행되었습니다. (trigger={})", trigger)
            BankCrawlRunResult.DuplicateRun
        } catch (e: Exception) {
            logger.error("은행 크롤링 Job 실행 중 오류가 발생했습니다. (trigger={})", trigger, e)
            applicationEventPublisher.publishEvent(OccurredCrawlErrorEvent("Bank", e.message ?: ""))
            BankCrawlRunResult.Failed
        }

    private fun launchUnlessRunning(trigger: BankCrawlTrigger): BankCrawlRunResult {
        val activeExecutions = failStaleExecutions(jobExplorer.findRunningJobExecutions(BankCrawlJobConfiguration.JOB_NAME))
        if (activeExecutions.isNotEmpty()) {
            logger.info("은행 크롤링 Job이 이미 실행 중입니다. (trigger={})", trigger)
            return BankCrawlRunResult.AlreadyRunning
        }

        val jobExecution = jobLauncher.run(bankCrawlJob, jobParametersFor(trigger))
        if (jobExecution.status != BatchStatus.COMPLETED) {
            return BankCrawlRunResult.Failed
        }
        return BankCrawlRunResult.Completed(jobExecution.executionContext.getInt(BankCrawlJobConfiguration.DEPOSIT_COUNT, 0))
    }

    private fun failStaleExecutions(runningExecutions: Set<JobExecution>): List<JobExecution> {
        val staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES)
        val (staleExecutions, activeExecutions) = runningExecutions.partition {
            (it.startTime ?: it.createTime).isBefore(staleThreshold)
        }

        staleExecutions.forEach(::markAsFailed)
        return activeExecutions
    }

    private fun markAsFailed(jobExecution: JobExecution) {
        val now = LocalDateTime.now()
        val staleExitStatus = ExitStatus.FAILED.addExitDescription(STALE_DESCRIPTION)

        jobExecution.stepExecutions
            .filter { it.status.isRunning }
            .forEach {
                it.status = BatchStatus.FAILED
                it.exitStatus = staleExitStatus
                it.setEndTime(now)
                jobRepository.update(it)
            }

        jobExecution.status = BatchStatus.FAILED
        jobExecution.exitStatus = staleExitStatus
        jobExecution.setEndTime(now)
        jobRepository.update(jobExecution)

        logger.warn("오래 실행 중인 은행 크롤링 Job을 실패 처리했습니다. (executionId={}, startTime={})", jobExecution.id, jobExecution.startTime)
    }

    private fun jobParametersFor(trigger: BankCrawlTrigger): JobParameters {
        val now = LocalDateTime.now(KST_ZONE)
        val requestedAt = if (trigger == BankCrawlTrigger.SCHEDULED) now.truncatedTo(ChronoUnit.MINUTES) else now

        return JobParametersBuilder()
            .addLocalDateTime(BankCrawlJobConfiguration.REQUESTED_AT, requestedAt, true)
            .addString(BankCrawlJobConfiguration.TRIGGER, trigger.name, false)
            .toJobParameters()
    }

    companion object {
        private val KST_ZONE = ZoneId.of("Asia/Seoul")
        private const val STALE_MINUTES = 30L
        private const val STALE_DESCRIPTION = "stale execution"
    }
}
