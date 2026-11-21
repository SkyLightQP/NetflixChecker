package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.crawler.BankCrawler
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BankCrawlJobConfiguration(
    private val jobRepository: JobRepository,
    @Qualifier("batchTransactionManager") private val batchTransactionManager: PlatformTransactionManager,
    private val bankCrawler: BankCrawler,
    private val browserSession: BankBrowserSession,
    private val bankStepRetry: BankStepRetry,
    private val bankCrawlResult: BankCrawlResult
) {
    companion object {
        const val JOB_NAME = "bankCrawlJob"
        const val REQUESTED_AT = "requestedAt"
        const val TRIGGER = "trigger"
        const val DEPOSIT_COUNT = "depositCount"

        private const val LOGIN_STEP = "bankLoginStep"
        private const val ACCOUNT_SELECT_STEP = "bankAccountSelectStep"
        private const val ACCOUNT_INQUIRY_STEP = "bankAccountInquiryStep"
    }

    @Bean
    fun bankCrawlJob(
        bankMaintenanceWindowValidator: BankMaintenanceWindowValidator,
        bankCrawlJobListener: BankCrawlJobListener
    ): Job = JobBuilder(JOB_NAME, jobRepository)
        .validator(bankMaintenanceWindowValidator)
        .listener(bankCrawlJobListener)
        .preventRestart()
        .start(bankLoginStep())
        .next(bankAccountSelectStep())
        .next(bankAccountInquiryStep())
        .build()

    @Bean
    fun bankLoginStep(): Step = StepBuilder(LOGIN_STEP, jobRepository)
        .tasklet({ _, _ ->
            bankStepRetry.run(LOGIN_STEP) { isRetry -> bankCrawler.login(browserSession.driver(), isRetry) }
            RepeatStatus.FINISHED
        }, batchTransactionManager)
        .build()

    @Bean
    fun bankAccountSelectStep(): Step = StepBuilder(ACCOUNT_SELECT_STEP, jobRepository)
        .tasklet({ _, _ ->
            bankStepRetry.run(ACCOUNT_SELECT_STEP) { isRetry ->
                bankCrawler.selectAccount(browserSession.driver(), isRetry)
            }
            RepeatStatus.FINISHED
        }, batchTransactionManager)
        .build()

    @Bean
    fun bankAccountInquiryStep(): Step = StepBuilder(ACCOUNT_INQUIRY_STEP, jobRepository)
        .tasklet({ _, chunkContext ->
            val deposits = bankStepRetry.run(ACCOUNT_INQUIRY_STEP) { isRetry ->
                bankCrawler.readDeposits(browserSession.driver(), isRetry)
            }
            bankCrawlResult.record(deposits)
            chunkContext.stepContext.stepExecution.jobExecution.executionContext.putInt(DEPOSIT_COUNT, deposits.size)
            RepeatStatus.FINISHED
        }, batchTransactionManager)
        .build()
}
