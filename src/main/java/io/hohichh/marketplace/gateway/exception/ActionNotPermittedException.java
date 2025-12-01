package io.hohichh.marketplace.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ActionNotPermittedException extends RuntimeException {
    public ActionNotPermittedException(String message) {
        super(message);
    }
}