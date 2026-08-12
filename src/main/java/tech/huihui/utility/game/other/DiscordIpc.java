package tech.huihui.utility.game.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DiscordIpc {
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private static final int OP_CLOSE = 2;
   private static final int HANDSHAKE_TIMEOUT_MS = 3000;
   private static final int MAX_FRAME_LENGTH = 1048576;

   private static final Object LOCK = new Object();
   private static IpcPipe pipe;
   private static String appId;
   private static boolean connected;
   private static Thread readerThread;
   private static volatile boolean running;

   private DiscordIpc() {
   }

   public static boolean start(String id) {
      stop();
      appId = id;
      IpcPipe candidate = openPipe();
      if (candidate == null) {
         return false;
      }

      final IpcPipe pipeRef = candidate;
      final boolean[] handshakeResult = new boolean[1];
      Thread handshakeThread = new Thread(() -> handshakeResult[0] = handshake(pipeRef, id), "Discord-Ipc-Handshake");
      handshakeThread.setDaemon(true);
      handshakeThread.start();

      try {
         handshakeThread.join(HANDSHAKE_TIMEOUT_MS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }

      if (!handshakeResult[0]) {
         closeQuietly(candidate);
         return false;
      }

      synchronized (LOCK) {
         pipe = candidate;
         connected = true;
      }

      running = true;
      readerThread = new Thread(DiscordIpc::readLoop, "Discord-Ipc-Reader");
      readerThread.setDaemon(true);
      readerThread.start();
      return true;
   }

   public static void stop() {
      running = false;
      IpcPipe current;
      synchronized (LOCK) {
         current = pipe;
         pipe = null;
         connected = false;
      }
      closeQuietly(current);
      if (readerThread != null) {
         readerThread.interrupt();
         readerThread = null;
      }
   }

   public static boolean update(JsonObject activity) {
      synchronized (LOCK) {
         if (!connected && appId != null && !appId.isBlank()) {
            start(appId);
         }
         if (!connected || pipe == null) {
            return false;
         }

         try {
            JsonObject frame = new JsonObject();
            frame.addProperty("cmd", "SET_ACTIVITY");
            frame.addProperty("nonce", UUID.randomUUID().toString());
            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);
            frame.add("args", args);
            writeFrame(pipe, OP_FRAME, frame.toString());
            return true;
         } catch (IOException e) {
            connected = false;
            IpcPipe dead = pipe;
            pipe = null;
            closeQuietly(dead);
            return false;
         }
      }
   }

   public static void clear() {
      JsonObject activity = new JsonObject();
      activity.addProperty("state", "");
      activity.addProperty("details", "");
      activity.add("assets", new JsonObject());
      update(activity);
   }

   private static void readLoop() {
      IpcPipe current;
      synchronized (LOCK) {
         current = pipe;
      }
      if (current == null) {
         return;
      }

      try {
         while (running) {
            JsonObject frame = readFrame(current);
            if (frame == null) {
               break;
            }
            if (frame.get("op") != null && frame.get("op").getAsInt() == OP_CLOSE) {
               break;
            }
         }
      } catch (Throwable ignored) {
      }

      if (running) {
         synchronized (LOCK) {
            connected = false;
            pipe = null;
         }
         closeQuietly(current);
      }
   }

   private static IpcPipe openPipe() {
      String os = System.getProperty("os.name", "").toLowerCase();
      if (os.contains("win")) {
         return openWindowsPipe();
      }
      return openUnixPipe();
   }

   private static IpcPipe openUnixPipe() {
      String[] candidates = new String[3];
      String xdg = System.getenv("XDG_RUNTIME_DIR");
      String tmp = System.getenv("TMPDIR");
      int count = 0;
      if (xdg != null && !xdg.isBlank()) {
         candidates[count++] = xdg + "/discord-ipc-0";
      }
      if (tmp != null && !tmp.isBlank()) {
         candidates[count++] = tmp + "/discord-ipc-0";
      }
      candidates[count++] = "/tmp/discord-ipc-0";

      for (int i = 0; i < count; i++) {
         try {
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(candidates[i]));
            return new UnixPipe(channel);
         } catch (IOException ignored) {
         }
      }
      return null;
   }

   private static IpcPipe openWindowsPipe() {
      try {
         WinNT.HANDLE handle = Kernel32.INSTANCE.CreateFile("\\\\.\\pipe\\discord-ipc-0", WinNT.GENERIC_READ | WinNT.GENERIC_WRITE, 0, null, WinNT.OPEN_EXISTING, 0, null);
         if (handle == null || WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
            return null;
         }
         return new WindowsPipe(handle);
      } catch (Throwable throwable) {
         return null;
      }
   }

   private static boolean handshake(IpcPipe candidate, String id) {
      try {
         JsonObject handshake = new JsonObject();
         handshake.addProperty("v", 1);
         handshake.addProperty("client_id", id);
         writeFrame(candidate, OP_HANDSHAKE, handshake.toString());
         JsonObject frame = readFrame(candidate);
         if (frame == null) {
            return false;
         }
         if (frame.get("op") == null || frame.get("op").getAsInt() != OP_FRAME) {
            return false;
         }
         return frame.get("evt") != null && "READY".equals(frame.get("evt").getAsString());
      } catch (IOException e) {
         return false;
      }
   }

   private static void writeFrame(IpcPipe target, int op, String json) throws IOException {
      byte[] payload = json.getBytes(StandardCharsets.UTF_8);
      ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      header.putInt(op);
      header.putInt(payload.length);
      writeFully(target, header.array(), 0, 8);
      writeFully(target, payload, 0, payload.length);
   }

   private static JsonObject readFrame(IpcPipe target) throws IOException {
      byte[] header = new byte[8];
      readFully(target, header, 0, 8);
      ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
      int op = buffer.getInt();
      int length = buffer.getInt();
      if (length < 0 || length > MAX_FRAME_LENGTH) {
         throw new IOException("Invalid frame length: " + length);
      }
      byte[] payload = new byte[length];
      readFully(target, payload, 0, length);
      JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
      json.addProperty("op", op);
      return json;
   }

   private static void writeFully(IpcPipe target, byte[] buf, int off, int len) throws IOException {
      target.write(buf, off, len);
   }

   private static void readFully(IpcPipe target, byte[] buf, int off, int len) throws IOException {
      int filled = 0;
      while (filled < len) {
         int n = target.read(buf, off + filled, len - filled);
         if (n < 0) {
            throw new IOException("Connection closed");
         }
         filled += n;
      }
   }

   private static void closeQuietly(IpcPipe target) {
      if (target != null) {
         try {
            target.close();
         } catch (Throwable ignored) {
         }
      }
   }

   private interface IpcPipe {
      int read(byte[] buf, int off, int len) throws IOException;

      void write(byte[] buf, int off, int len) throws IOException;

      void close() throws IOException;
   }

   private static final class UnixPipe implements IpcPipe {
      private final SocketChannel channel;

      UnixPipe(SocketChannel channel) {
         this.channel = channel;
      }

      public int read(byte[] buf, int off, int len) throws IOException {
         return this.channel.read(ByteBuffer.wrap(buf, off, len));
      }

      public void write(byte[] buf, int off, int len) throws IOException {
         ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
         while (buffer.hasRemaining()) {
            this.channel.write(buffer);
         }
      }

      public void close() throws IOException {
         this.channel.close();
      }
   }

   private static final class WindowsPipe implements IpcPipe {
      private final WinNT.HANDLE handle;

      WindowsPipe(WinNT.HANDLE handle) {
         this.handle = handle;
      }

      public int read(byte[] buf, int off, int len) throws IOException {
         IntByReference read = new IntByReference();
         if (!Kernel32.INSTANCE.ReadFile(this.handle, buf, len, read, null)) {
            throw new IOException("ReadFile failed: " + Kernel32.INSTANCE.GetLastError());
         }
         return read.getValue();
      }

      public void write(byte[] buf, int off, int len) throws IOException {
         IntByReference written = new IntByReference();
         if (!Kernel32.INSTANCE.WriteFile(this.handle, buf, len, written, null)) {
            throw new IOException("WriteFile failed: " + Kernel32.INSTANCE.GetLastError());
         }
      }

      public void close() {
         Kernel32.INSTANCE.CloseHandle(this.handle);
      }
   }
}
