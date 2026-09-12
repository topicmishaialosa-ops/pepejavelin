package org.blade.event;

record ListenerRegistration(
        Class<? extends Event> eventType,
        RegisteredListener listener
) {
}
