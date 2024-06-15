package me.daegyeo.netflixchecker.api.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.daegyeo.netflixchecker.api.controller.request.AddDepositorRequest
import me.daegyeo.netflixchecker.api.dto.DepositLogDTO
import me.daegyeo.netflixchecker.api.service.DepositService
import me.daegyeo.netflixchecker.data.AccountData
import me.daegyeo.netflixchecker.shared.annotation.CheckAuth
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/deposit")
class DepositController(
    private val depositService: DepositService,
    private val coroutineScope: CoroutineScope
) {

    @CheckAuth
    @GetMapping
    fun getDepositors(): Map<String, List<DepositLogDTO>> {
        return mapOf("result" to depositService.getDepositors())
    }

    @CheckAuth
    @PostMapping("/crawl")
    fun crawlBankManually(): ResponseEntity<Unit> {
        coroutineScope.launch {
            depositService.crawlBankManually()
        }
        return ResponseEntity.ok().build()
    }

    @CheckAuth
    @PostMapping
    fun addDepositor(@RequestBody request: AddDepositorRequest) {
        val now = LocalDateTime.now()
        depositService.addDepositor(
            AccountData(
                who = request.who,
                cost = request.cost.toString(),
                costMonth = request.costMonth.toString(),
                date = now.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                time = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            )
        )
    }
}