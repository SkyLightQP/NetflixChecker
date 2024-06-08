package me.daegyeo.netflixchecker.api.controller

import me.daegyeo.netflixchecker.api.service.MetricsService
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metrics")
class MetricsController(private val metricsService: MetricsService) {

    @CheckAuth
    @GetMapping("/deposit/count")
    fun getThisMonthDepositCount(): Map<String, Int> {
        return mapOf("result" to metricsService.getThisMonthDepositCount())
    }

    @CheckAuth
    @GetMapping("/deposit/latest")
    fun getLatestDepositor(): Map<String, String> {
        return mapOf("result" to metricsService.getLatestDepositor())
    }

    @CheckAuth
    @GetMapping("/code/count")
    fun getThisMonthCodeGeneratedCount(): Map<String, Int> {
        return mapOf("result" to metricsService.getThisMonthCodeGeneratedCount())
    }

    @CheckAuth
    @GetMapping("/crawling/status")
    fun getIsSuccessLatestCrawling(): Map<String, String> {
        return mapOf("result" to if (metricsService.getIsSuccessLatestCrawling()) "O" else "X")
    }

    @CheckAuth
    @GetMapping("/crawling/latest")
    fun getLatestCrawlingTime(): Map<String, String> {
        return mapOf("result" to metricsService.getLatestCrawlingTime())
    }
}