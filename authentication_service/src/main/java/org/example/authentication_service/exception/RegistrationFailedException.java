package org.example.authentication_service.exception;

public class RegistrationFailedException extends RuntimeException{
    public RegistrationFailedException(String message){
        super(message);
    }

    public RegistrationFailedException(String message, Throwable cause){
        super(message,cause);
    }
}
