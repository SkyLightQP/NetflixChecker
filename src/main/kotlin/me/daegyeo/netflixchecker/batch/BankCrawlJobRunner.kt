package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.event.OccurredCrawlErrorEvent
import me.daegyeo.netflixchecker.shared.lock.AdvisoryLockExecutor
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
    private val advisoryLockExecutor: AdvisoryLockExecutor,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    companion object {
        private val KST_ZONE = ZoneId.of("Asia/Seoul")
        private const val CRAWL_LOCK_KEY = 6_158_204_771_003L
        private const val ORPHANED_DESCRIPTION = "orphaned execution"
    }

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
            alertCrawlFailure(e.message ?: "")
            BankCrawlRunResult.Failed
        }

    private fun alertCrawlFailure(message: String) {
        runCatching {
            applicationEventPublisher.publishEvent(OccurredCrawlErrorEvent("Bank", message))
        }.onFailure {
            logger.error("은행 크롤링 오류 알림 발행 중 오류가 발생했습니다.", it)
        }
    }

    private fun launchUnlessRunning(trigger: BankCrawlTrigger): BankCrawlRunResult =
        advisoryLockExecutor.runExclusively(CRAWL_LOCK_KEY) { launch(trigger) } ?: run {
            logger.info("은행 크롤링 Job이 이미 실행 중입니다. (trigger={})", trigger)
            BankCrawlRunResult.AlreadyRunning
        }

    private fun launch(trigger: BankCrawlTrigger): BankCrawlRunResult {
        jobExplorer.findRunningJobExecutions(BankCrawlJobConfiguration.JOB_NAME).forEach(::markAsOrphaned)

        val jobExecution = jobLauncher.run(bankCrawlJob, jobParametersFor(trigger))
        if (jobExecution.status != BatchStatus.COMPLETED) {
            return BankCrawlRunResult.Failed
        }
        return BankCrawlRunResult.Completed(jobExecution.executionContext.getInt(BankCrawlJobConfiguration.DEPOSIT_COUNT, 0))
    }

    private fun markAsOrphaned(jobExecution: JobExecution) {
        val now = LocalDateTime.now()
        val orphanedExitStatus = ExitStatus.FAILED.addExitDescription(ORPHANED_DESCRIPTION)

        jobExecution.stepExecutions
            .filter { it.status.isRunning }
            .forEach {
                it.status = BatchStatus.FAILED
                it.exitStatus = orphanedExitStatus
                it.setEndTime(now)
                jobRepository.update(it)
            }

        jobExecution.status = BatchStatus.FAILED
        jobExecution.exitStatus = orphanedExitStatus
        jobExecution.setEndTime(now)
        jobRepository.update(jobExecution)

        logger.warn("중단된 채 남아 있던 은행 크롤링 Job을 실패 처리했습니다. (executionId={}, startTime={})", jobExecution.id, jobExecution.startTime)
    }

    private fun jobParametersFor(trigger: BankCrawlTrigger): JobParameters {
        val now = LocalDateTime.now(KST_ZONE)
        val requestedAt = if (trigger == BankCrawlTrigger.SCHEDULED) now.truncatedTo(ChronoUnit.MINUTES) else now

        return JobParametersBuilder()
            .addLocalDateTime(BankCrawlJobConfiguration.REQUESTED_AT, requestedAt, true)
            .addString(BankCrawlJobConfiguration.TRIGGER, trigger.name, false)
            .toJobParameters()
    }
}
