package org.blade.event.events.input;

import lombok.Getter;
import org.blade.event.CancellableEvent;

@Getter
public final class CharacterInputEvent extends CancellableEvent {
    private final long window;
    private final int codePoint;

    public CharacterInputEvent(long window, int codePoint) {
        this.window = window;
        this.codePoint = codePoint;
    }
}
