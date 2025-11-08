package com.example.resqnet_app.utils;

public class Event<T> {
    private final T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content only if it has not been handled yet.
     * After that, it marks the content as handled.
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) return null;
        hasBeenHandled = true;
        return content;
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    public T peekContent() {
        return content;
    }
}
