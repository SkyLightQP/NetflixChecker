package me.daegyeo.netflixchecker.advice

import me.daegyeo.netflixchecker.api.exception.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionHandleAdvice {
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ResponseEntity<Map<String, String>> {
        val response = mapOf("message" to ex.message)
        return ResponseEntity(response, HttpStatus.valueOf(ex.httpStatus))
    }
}