package org.example.user_service.exception;

public class EmailAlreadyExistsException extends IllegalArgumentException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
