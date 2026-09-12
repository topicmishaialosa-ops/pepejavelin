package org.blade.event.events.input;

import lombok.Getter;
import org.blade.event.CancellableEvent;

@Getter
public final class ScrollInputEvent extends CancellableEvent {
    private final double vertical;

    public ScrollInputEvent(double vertical) {
        this.vertical = vertical;
    }
}