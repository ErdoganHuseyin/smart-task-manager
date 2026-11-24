package com.erdoganhuseyin.taskmanager.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        // Same message for "no such email" and "wrong password",
        // so an attacker cannot find out which emails are registered.
        super("Invalid email or password");
    }
}
