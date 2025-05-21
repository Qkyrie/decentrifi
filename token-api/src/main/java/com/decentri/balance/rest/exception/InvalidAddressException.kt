package com.decentri.balance.rest.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid address")
class InvalidAddressException(override val message: String) : RuntimeException(message)
