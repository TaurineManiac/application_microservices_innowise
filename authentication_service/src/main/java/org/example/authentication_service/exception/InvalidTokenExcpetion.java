package org.example.authentication_service.exception;

public class InvalidTokenExcpetion extends RuntimeException {
  public InvalidTokenExcpetion(String message) {
    super(message);
  }
}
