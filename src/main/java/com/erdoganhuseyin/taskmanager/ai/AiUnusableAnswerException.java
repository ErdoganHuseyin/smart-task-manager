package com.erdoganhuseyin.taskmanager.ai;

/** Claude answered, but we cannot use the answer (it declined, or the result is empty). Becomes HTTP 422. */
public class AiUnusableAnswerException extends RuntimeException {

    public AiUnusableAnswerException(String message) {
        super(message);
    }
}
