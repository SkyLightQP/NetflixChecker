package me.daegyeo.netflixchecker.api.dto

import java.time.LocalDate

data class DepositLogDTO(
    val id: Int,
    val who: String,
    val cost: Int,
    val date: LocalDate,
    val costMonth: Int
)
