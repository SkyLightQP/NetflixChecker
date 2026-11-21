package me.daegyeo.netflixchecker.batch

import me.daegyeo.netflixchecker.data.AccountData
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component

@Component
@JobScope
class BankCrawlResult {
    private var recordedDeposits: List<AccountData> = emptyList()

    val deposits: List<AccountData>
        get() = recordedDeposits

    fun record(deposits: List<AccountData>) {
        recordedDeposits = deposits
    }
}
