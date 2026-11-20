package com.example.tasktracker.exception;

public class InviteCodeNotExistsException extends RuntimeException {
    public InviteCodeNotExistsException(String inviteCode) {
        super("Organization with invite code " + inviteCode + " not exists");
    }
}
