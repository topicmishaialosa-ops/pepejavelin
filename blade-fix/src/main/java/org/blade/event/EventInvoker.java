package org.blade.event;

@FunctionalInterface
public interface EventInvoker {
    void invoke(Object target, Event event);
}
