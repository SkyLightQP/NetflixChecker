package me.daegyeo.netflixchecker.api.controller.request

data class AddDepositorRequest (
    val who: String,
    val cost: Int,
    val costMonth: Int,
    val date: String
)