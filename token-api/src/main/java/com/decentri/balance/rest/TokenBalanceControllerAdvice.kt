package com.decentri.balance.rest

import com.decentri.balance.rest.exception.InvalidAddressException
import com.decentri.balance.rest.response.ErrorResponse
import com.decentri.balance.rest.response.errorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class TokenBalanceControllerAdvice {

    @ExceptionHandler(InvalidAddressException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBookNotFoundException(ex: InvalidAddressException): ResponseEntity<ErrorResponse> {
        return ResponseEntity<ErrorResponse>(errorResponse(ex.message), HttpStatus.BAD_REQUEST)
    }
}