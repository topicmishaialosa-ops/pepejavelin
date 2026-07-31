package tech.huihui.base.events.impl.input;

import lombok.Generated;
import tech.huihui.base.events.callables.EventCancellable;

public final class EventChatSend extends EventCancellable {
   private String message;

   @Generated
   public String getMessage() {
      return this.message;
   }

   @Generated
   public EventChatSend(String message) {
      this.message = message;
   }

   @Generated
   public void setMessage(String message) {
      this.message = message;
   }
}
