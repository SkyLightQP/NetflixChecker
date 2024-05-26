package me.daegyeo.netflixchecker.api.exception

class ServiceException(override val message: String, val httpStatus: Int) : RuntimeException(message)