package com.erdoganhuseyin.taskmanager.task;

public class InvalidSortException extends RuntimeException {
    public InvalidSortException(String property) {
        super("Invalid sort property " + property);
    }
}
