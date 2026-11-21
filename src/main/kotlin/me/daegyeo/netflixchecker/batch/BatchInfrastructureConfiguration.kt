package me.daegyeo.netflixchecker.batch

import org.springframework.boot.autoconfigure.batch.BatchTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.sql.DataSource

@Configuration
class BatchInfrastructureConfiguration {
    @Bean(defaultCandidate = false)
    @BatchTransactionManager
    fun batchTransactionManager(dataSource: DataSource): DataSourceTransactionManager =
        DataSourceTransactionManager(dataSource)
}
