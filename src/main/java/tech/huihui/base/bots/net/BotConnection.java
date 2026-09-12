package tech.huihui.base.bots.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import tech.huihui.base.bots.InstanceBot;

public final class BotConnection {
   public enum State {
      OFFLINE,
      CONNECTING,
      CONFIG,
      PLAY,
      DISCONNECTED,
      ERROR
   }

   private static final int PROTOCOL_VERSION = 769;

   private final InstanceBot bot;
   private final String host;
   private final int port;
   private final Runnable onState;
   private final Runnable onMessage;

   private volatile State state = State.OFFLINE;
   private volatile String error = "";
   private volatile boolean running;

   private volatile double x;
   private volatile double y;
   private volatile double z;
   private volatile float yaw;
   private volatile float pitch;
   private volatile int entityId = -1;

   private final Object writeLock = new Object();
   private Socket socket;
   private volatile OutputStream out;
   private volatile InputStream in;
   private volatile int compressionThreshold = -1;

   private final AtomicBoolean forward = new AtomicBoolean();
   private final AtomicBoolean back = new AtomicBoolean();
   private final AtomicBoolean left = new AtomicBoolean();
   private final AtomicBoolean right = new AtomicBoolean();
   private final AtomicBoolean jump = new AtomicBoolean();
   private final AtomicBoolean sneak = new AtomicBoolean();
   private final AtomicBoolean sprint = new AtomicBoolean();

   private float lookYawDelta;
   private float lookPitchDelta;
   private boolean sneakSent;
   private boolean sprintSent;

   private final ConcurrentLinkedQueue<String> outboundChat = new ConcurrentLinkedQueue<>();
   private final List<String> chatLines = new ArrayList<>();

   public BotConnection(InstanceBot bot, Runnable onState, Runnable onMessage) {
      this.bot = bot;
      String server = bot.getServer();
      int idx = server.lastIndexOf(':');
      if (idx > 0 && server.indexOf(':') == idx) {
         this.host = server.substring(0, idx);
         this.port = this.parsePort(server.substring(idx + 1));
      } else {
         this.host = server.isEmpty() ? "localhost" : server;
         this.port = 25565;
      }
      this.onState = onState;
      this.onMessage = onMessage;
   }

   private int parsePort(String s) {
      String value = s.replaceAll("\\s+", "");
      if (value.isEmpty()) {
         return 25565;
      }
      try {
         int p = Integer.parseInt(value);
         return p > 0 && p <= 65535 ? p : 25565;
      } catch (NumberFormatException e) {
         return 25565;
      }
   }

   public void start() {
      if (this.running) {
         return;
      }
      this.running = true;
      this.setState(State.CONNECTING);
      Thread thread = new Thread(this::run, "huihui-bot-" + this.bot.getName());
      thread.setDaemon(true);
      thread.start();
      Thread movement = new Thread(this::movementLoop, "huihui-bot-move-" + this.bot.getName());
      movement.setDaemon(true);
      movement.start();
   }

   private void run() {
      Thread movement = null;
      try {
         this.socket = new Socket();
         this.socket.connect(new InetSocketAddress(this.host, this.port), 10000);
         this.socket.setTcpNoDelay(true);
         this.in = new BufferedInputStream(this.socket.getInputStream());
         this.out = new BufferedOutputStream(this.socket.getOutputStream());

         this.sendHandshake();
         this.sendLoginStart();

         while (this.running && this.readable()) {
            this.handlePacket();
         }
      } catch (Exception e) {
         if (this.state != State.DISCONNECTED && this.running) {
            this.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            this.setState(State.ERROR);
         }
      } finally {
         this.running = false;
         if (movement != null) {
            movement.interrupt();
         }
         this.closeSocket();
         if (this.state == State.CONNECTING || this.state == State.CONFIG || this.state == State.PLAY) {
            this.setState(State.DISCONNECTED);
         }
      }
   }

   private boolean readable() {
      return this.state == State.CONNECTING || this.state == State.CONFIG || this.state == State.PLAY;
   }

   private void sendHandshake() throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0);
      out.writeVarInt(PROTOCOL_VERSION);
      out.writeString(this.host);
      out.writeUnsignedShort(this.port);
      out.writeVarInt(2);
      this.flush(out);
   }

   private void sendLoginStart() throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0);
      out.writeString(this.bot.getName());
      out.writeBytes(this.uuidBytes(this.bot.getUuid()));
      this.flush(out);
   }

   private byte[] uuidBytes(UUID uuid) {
      byte[] bytes = new byte[16];
      long msb = uuid.getMostSignificantBits();
      long lsb = uuid.getLeastSignificantBits();
      for (int i = 7; i >= 0; i--) {
         bytes[i] = (byte) (msb & 0xFFL);
         msb >>= 8;
      }
      for (int i = 15; i >= 8; i--) {
         bytes[i] = (byte) (lsb & 0xFFL);
         lsb >>= 8;
      }
      return bytes;
   }

   private void handlePacket() throws IOException {
      byte[] payload = this.readFrame();
      In data = new In(payload);
      int id = data.readVarInt();
      switch (this.state) {
         case CONFIG:
            this.handleConfig(data, id);
            break;
         case PLAY:
            this.handlePlay(data, id);
            break;
         default:
            this.handleLogin(data, id);
            break;
      }
   }

   private void handleLogin(In data, int id) throws IOException {
      switch (id) {
         case 0x00:
            this.error = "Отказ сервера: " + data.readString();
            this.setState(State.ERROR);
            break;
         case 0x01:
            this.error = "Сервер в онлайн-режиме (VIP) не поддерживается";
            this.setState(State.ERROR);
            break;
         case 0x02:
            data.readUuid();
            data.readString();
            int props = data.readVarInt();
            for (int i = 0; i < props; i++) {
               data.readString();
               data.readString();
               if (data.readBoolean()) {
                  data.readString();
               }
            }
            this.setState(State.CONFIG);
            this.flush(this.emptyPacket(0x03));
            this.flush(this.clientSettings());
            break;
         case 0x03:
            this.compressionThreshold = data.readVarInt();
            break;
         default:
            break;
      }
   }

   private Packet emptyPacket(int id) {
      Packet out = new Packet();
      out.writeVarInt(id);
      return out;
   }

   private Packet clientSettings() {
      Packet out = new Packet();
      out.writeVarInt(0x00);
      out.writeString("en_us");
      out.writeByte(8);
      out.writeVarInt(0);
      out.writeBoolean(true);
      out.writeUnsignedByte(0);
      out.writeVarInt(1);
      out.writeBoolean(false);
      out.writeBoolean(true);
      out.writeVarInt(0);
      return out;
   }

   private void handleConfig(In data, int id) throws IOException {
      switch (id) {
         case 0x00:
            String key = data.readString();
            Packet cookie = new Packet();
            cookie.writeVarInt(0x01);
            cookie.writeString(key);
            cookie.writeBoolean(false);
            this.flush(cookie);
            break;
         case 0x03:
            this.flush(this.emptyPacket(0x03));
            this.setState(State.PLAY);
            this.pushChat("Бот " + this.bot.getName() + " в игре");
            break;
         case 0x04:
            long keepAliveId = data.readLong();
            Packet reply = new Packet();
            reply.writeVarInt(0x04);
            reply.writeLong(keepAliveId);
            this.flush(reply);
            break;
         case 0x0e: {
            int packCount = data.readVarInt();
            for (int i = 0; i < packCount; i++) {
               data.readString();
               data.readString();
               data.readString();
            }
            Packet packs = new Packet();
            packs.writeVarInt(0x07);
            packs.writeVarInt(0);
            this.flush(packs);
            break;
         }
         default:
            break;
      }
   }

   private void handlePlay(In data, int id) throws IOException {
      switch (id) {
         case 0x1d:
            this.error = this.readTextComponent(data);
            if (this.error.isEmpty()) {
               this.error = "Отключено сервером";
            }
            this.setState(State.DISCONNECTED);
            break;
         case 0x27:
            long keepAlive = data.readLong();
            Packet reply = new Packet();
            reply.writeVarInt(0x1a);
            reply.writeLong(keepAlive);
            this.flush(reply);
            break;
         case 0x2c:
            this.entityId = data.readInt();
            break;
         case 0x42:
            this.applyTeleport(data);
            break;
         case 0x43:
            this.yaw = data.readFloat();
            this.pitch = data.readFloat();
            break;
         case 0x73:
            String text = this.readTextComponent(data);
            if (!text.isEmpty()) {
               this.pushChat(text);
            }
            break;
         default:
            break;
      }
   }

   private void applyTeleport(In data) throws IOException {
      int teleportId = data.readVarInt();
      double tx = data.readDouble();
      double ty = data.readDouble();
      double tz = data.readDouble();
      data.readDouble();
      data.readDouble();
      data.readDouble();
      float tyaw = data.readFloat();
      float tpitch = data.readFloat();
      int flags = data.readInt();
      if ((flags & 0x01) != 0) {
         tx += this.x;
      }
      if ((flags & 0x02) != 0) {
         ty += this.y;
      }
      if ((flags & 0x04) != 0) {
         tz += this.z;
      }
      if ((flags & 0x08) != 0) {
         tyaw += this.yaw;
      }
      if ((flags & 0x10) != 0) {
         tpitch += this.pitch;
      }
      this.x = tx;
      this.y = ty;
      this.z = tz;
      this.yaw = tyaw;
      this.pitch = tpitch;
      Packet confirm = new Packet();
      confirm.writeVarInt(0x00);
      confirm.writeVarInt(teleportId);
      this.flush(confirm);
   }

   public void sendChat(String message) {
      if (message == null) {
         return;
      }
      String msg = message.trim();
      if (msg.isEmpty()) {
         return;
      }
      if (msg.length() > 256) {
         msg = msg.substring(0, 256);
      }
      this.outboundChat.add(msg);
   }

   private void movementLoop() {
      while (this.running) {
         if (this.state == State.PLAY) {
            try {
               this.autopilot();
            } catch (Exception e) {
               if (this.state == State.PLAY) {
                  this.error = e.getMessage() == null ? "Сетевая ошибка" : e.getMessage();
                  this.setState(State.ERROR);
               }
               Thread.interrupted();
               return;
            }
         }
         try {
            Thread.sleep(50L);
         } catch (InterruptedException e) {
            return;
         }
      }
   }

   private void autopilot() throws IOException {
      String chat = this.outboundChat.poll();
      if (chat != null) {
         this.sendChatPacket(chat);
      }

      float yawDelta = this.lookYawDelta;
      float pitchDelta = this.lookPitchDelta;
      boolean lookChanged = yawDelta != 0.0F || pitchDelta != 0.0F;
      if (lookChanged) {
         this.lookYawDelta = 0.0F;
         this.lookPitchDelta = 0.0F;
         this.yaw = ((this.yaw + yawDelta) % 360.0F + 360.0F) % 360.0F;
         this.pitch = Math.max(-90.0F, Math.min(90.0F, this.pitch + pitchDelta));
      }

      boolean sneaking = this.sneak.get();
      boolean sprinting = this.sprint.get();
      if (sneaking && !this.sneakSent) {
         this.sendEntityAction(0);
         this.sneakSent = true;
      } else if (!sneaking && this.sneakSent) {
         this.sendEntityAction(1);
         this.sneakSent = false;
      }
      if (sprinting && !this.sprintSent) {
         this.sendEntityAction(3);
         this.sprintSent = true;
      } else if (!sprinting && this.sprintSent) {
         this.sendEntityAction(4);
         this.sprintSent = false;
      }

      boolean f = this.forward.get();
      boolean b = this.back.get();
      boolean l = this.left.get();
      boolean r = this.right.get();
      boolean moving = f || b || l || r;

      double speed = sneaking ? 1.3D : (sprinting ? 5.6D : 4.317D);
      double forwardAmt = (f ? 1.0D : 0.0D) - (b ? 1.0D : 0.0D);
      double strafeAmt = (r ? 1.0D : 0.0D) - (l ? 1.0D : 0.0D);
      double yawRad = Math.toRadians(this.yaw);
      double dirX = -Math.sin(yawRad) * forwardAmt + Math.cos(yawRad) * strafeAmt;
      double dirZ = Math.cos(yawRad) * forwardAmt + Math.sin(yawRad) * strafeAmt;
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      if (len > 1.0E-4D) {
         dirX /= len;
         dirZ /= len;
      }

      boolean jumped = false;
      if (this.jump.get()) {
         this.y += 0.42D;
         jumped = true;
      }

      double dist = speed * 0.05D;
      double nx = this.x + dirX * dist;
      double nz = this.z + dirZ * dist;
      if (moving) {
         this.x = nx;
         this.z = nz;
      }

      if (moving && lookChanged) {
         this.sendPositionLook();
      } else if (moving) {
         this.sendPosition();
      } else if (lookChanged) {
         this.sendLook();
      } else if (jumped) {
         this.sendPosition();
      }
   }

   private void sendEntityAction(int action) throws IOException {
      if (this.entityId < 0) {
         return;
      }
      Packet out = new Packet();
      out.writeVarInt(0x28);
      out.writeVarInt(this.entityId);
      out.writeVarInt(action);
      out.writeVarInt(0);
      this.flush(out);
   }

   private void sendPosition() throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0x1c);
      out.writeDouble(this.x);
      out.writeDouble(this.y);
      out.writeDouble(this.z);
      out.writeByte(1);
      this.flush(out);
   }

   private void sendPositionLook() throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0x1d);
      out.writeDouble(this.x);
      out.writeDouble(this.y);
      out.writeDouble(this.z);
      out.writeFloat(this.yaw);
      out.writeFloat(this.pitch);
      out.writeByte(1);
      this.flush(out);
   }

   private void sendLook() throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0x1e);
      out.writeFloat(this.yaw);
      out.writeFloat(this.pitch);
      out.writeByte(1);
      this.flush(out);
   }

   private void sendChatPacket(String msg) throws IOException {
      Packet out = new Packet();
      out.writeVarInt(0x07);
      out.writeString(msg);
      out.writeLong(System.currentTimeMillis() / 1000L);
      out.writeLong(0L);
      out.writeBoolean(false);
      out.writeVarInt(0);
      out.writeByte(0);
      out.writeByte(0);
      out.writeByte(0);
      this.flush(out);
   }

   public void swingArm() throws IOException {
      if (this.state != State.PLAY) {
         return;
      }
      Packet out = new Packet();
      out.writeVarInt(0x3a);
      out.writeVarInt(1);
      this.flush(out);
   }

   public void useItem() throws IOException {
      if (this.state != State.PLAY) {
         return;
      }
      Packet out = new Packet();
      out.writeVarInt(0x3d);
      out.writeVarInt(1);
      this.flush(out);
   }

   public void setKey(int keyCode, boolean down) {
      switch (keyCode) {
         case 87:
            this.forward.set(down);
            break;
         case 65:
            this.left.set(down);
            break;
         case 83:
            this.back.set(down);
            break;
         case 68:
            this.right.set(down);
            break;
         case 32:
            this.jump.set(down);
            break;
         case 340:
            this.sneak.set(down);
            break;
         case 341:
            this.sprint.set(down);
            break;
         default:
            break;
      }
   }

   public void clearKeys() {
      this.forward.set(false);
      this.back.set(false);
      this.left.set(false);
      this.right.set(false);
      this.jump.set(false);
      this.sneak.set(false);
      this.sprint.set(false);
   }

   public void addLookDelta(float dYaw, float dPitch) {
      if (this.state != State.PLAY) {
         return;
      }
      this.lookYawDelta += dYaw;
      this.lookPitchDelta += dPitch;
   }

   public void dispose() {
      this.running = false;
      this.closeSocket();
   }

   public List<String> getChatLines() {
      synchronized (this.chatLines) {
         return Collections.unmodifiableList(new ArrayList<>(this.chatLines));
      }
   }

   private void pushChat(String text) {
      if (text.isEmpty()) {
         return;
      }
      synchronized (this.chatLines) {
         this.chatLines.add(text);
         if (this.chatLines.size() > 30) {
            this.chatLines.remove(0);
         }
      }
   }

   public State getState() {
      return this.state;
   }

   public String getError() {
      return this.error;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public InstanceBot getBot() {
      return this.bot;
   }

   private void setState(State state) {
      this.state = state;
      if (this.onState != null) {
         try {
            this.onState.run();
         } catch (Exception ignored) {
         }
      }
   }

   private void closeSocket() {
      try {
         if (this.socket != null) {
            this.socket.close();
         }
      } catch (IOException ignored) {
      }
      this.in = null;
      this.out = null;
   }

   private void flush(Packet packet) throws IOException {
      if (!this.running) {
         return;
      }
      byte[] frame = this.frame(packet.toByteArray());
      synchronized (this.writeLock) {
         OutputStream stream = this.out;
         if (stream == null) {
            return;
         }
         stream.write(frame);
         stream.flush();
      }
   }

   private byte[] frame(byte[] payload) throws IOException {
      ByteArrayOutputStream frame = new ByteArrayOutputStream();
      int threshold = this.compressionThreshold;
      if (threshold >= 0) {
         int dataLen = payload.length;
         if (dataLen >= threshold) {
            Deflater deflater = new Deflater();
            try {
               deflater.setInput(payload);
               deflater.finish();
               byte[] buffer = new byte[4096];
               ByteArrayOutputStream comp = new ByteArrayOutputStream();
               while (!deflater.finished()) {
                  int count = deflater.deflate(buffer);
                  comp.write(buffer, 0, count);
               }
               byte[] compressed = comp.toByteArray();
               writeVarInt(frame, varIntSize(dataLen) + compressed.length);
               writeVarInt(frame, dataLen);
               frame.write(compressed, 0, compressed.length);
            } finally {
               deflater.end();
            }
         } else {
            writeVarInt(frame, varIntSize(0) + payload.length);
            writeVarInt(frame, 0);
            frame.write(payload, 0, payload.length);
         }
      } else {
         writeVarInt(frame, payload.length);
         frame.write(payload, 0, payload.length);
      }
      return frame.toByteArray();
   }

   private byte[] readFrame() throws IOException {
      InputStream stream = this.in;
      if (stream == null) {
         throw new IOException("Соединение закрыто");
      }
      int length = this.readVarInt(stream);
      if (length < 0 || length > 2097152) {
         throw new IOException("Слишком большой пакет: " + length);
      }
      byte[] frame = new byte[length];
      this.readFully(stream, frame);
      if (this.compressionThreshold < 0) {
         return frame;
      }
      In data = new In(frame);
      int dataLen = data.readVarInt();
      if (dataLen == 0) {
         return data.readRemaining();
      }
      byte[] compressed = data.readRemaining();
      Inflater inflater = new Inflater();
      try {
         inflater.setInput(compressed);
         byte[] outBuf = new byte[dataLen];
         int total = 0;
         while (total < dataLen) {
            int count = inflater.inflate(outBuf, total, dataLen - total);
            if (count == 0 && inflater.needsInput()) {
               break;
            }
            total += count;
         }
         if (total != dataLen) {
            throw new IOException("Сбой распаковки пакета");
         }
         return outBuf;
      } catch (java.util.zip.DataFormatException e) {
         throw new IOException("Сбой распаковки пакета", e);
      } finally {
         inflater.end();
      }
   }

   private int readVarInt(InputStream stream) throws IOException {
      int value = 0;
      for (int i = 0; i < 5; i++) {
         int b = stream.read();
         if (b == -1) {
            throw new EOFException();
         }
         value |= (b & 0x7F) << (7 * i);
         if ((b & 0x80) == 0) {
            return value;
         }
      }
      throw new IOException("VarInt слишком большой");
   }

   private void readFully(InputStream stream, byte[] buffer) throws IOException {
      int read = 0;
      while (read < buffer.length) {
         int count = stream.read(buffer, read, buffer.length - read);
         if (count == -1) {
            throw new EOFException();
         }
         read += count;
      }
   }

   private int varIntSize(int value) {
      int size = 0;
      do {
         value >>>= 7;
         size++;
      } while (value != 0);
      return size;
   }

   private static void writeVarInt(ByteArrayOutputStream out, int value) {
      while ((value & ~0x7F) != 0) {
         out.write((value & 0x7F) | 0x80);
         value >>>= 7;
      }
      out.write(value);
   }

   private String readTextComponent(In data) {
      try {
         StringBuilder sb = new StringBuilder();
         int type = data.data[data.pos++] & 0xFF;
         if (type == 8) {
            return readNbtString(data);
         }
         if (type == 10) {
            readCompound(data, sb);
         }
         return sb.toString();
      } catch (Exception e) {
         return "";
      }
   }

   private void readCompound(In data, StringBuilder text) {
      while (true) {
         int type = data.data[data.pos++] & 0xFF;
         if (type == 0) {
            return;
         }
         String name = readNbtString(data);
         if (type == 8) {
            String value = readNbtString(data);
            if ("text".equals(name)) {
               if (text.length() > 0) {
                  text.append(' ');
               }
               text.append(value);
            }
         } else {
            readNbtValue(data, type, text);
         }
      }
   }

   private void readNbtValue(In data, int type, StringBuilder text) {
      switch (type) {
         case 1:
            data.pos += 1;
            break;
         case 2:
            data.pos += 2;
            break;
         case 3:
            data.pos += 4;
            break;
         case 4:
            data.pos += 8;
            break;
         case 5:
            data.pos += 4;
            break;
         case 6:
            data.pos += 8;
            break;
         case 7: {
            int len = readIntBE(data);
            data.pos += len;
            break;
         }
         case 9: {
            int elementType = data.data[data.pos++] & 0xFF;
            int count = readIntBE(data);
            for (int i = 0; i < count; i++) {
               if (elementType == 10) {
                  readCompound(data, text);
               } else {
                  readNbtValue(data, elementType, text);
               }
            }
            break;
         }
         case 10:
            readCompound(data, text);
            break;
         case 11: {
            int len = readIntBE(data);
            data.pos += len * 4;
            break;
         }
         case 12: {
            int len = readIntBE(data);
            data.pos += len * 8;
            break;
         }
         default:
            break;
      }
   }

   private String readNbtString(In data) {
      int len = ((data.data[data.pos] & 0xFF) << 8) | (data.data[data.pos + 1] & 0xFF);
      String s = new String(data.data, data.pos + 2, len, StandardCharsets.UTF_8);
      data.pos += 2 + len;
      return s;
   }

   private static int readIntBE(In data) {
      int value = (data.data[data.pos] & 0xFF) << 24 | (data.data[data.pos + 1] & 0xFF) << 16
         | (data.data[data.pos + 2] & 0xFF) << 8 | (data.data[data.pos + 3] & 0xFF);
      data.pos += 4;
      return value;
   }

   private static final class Packet {
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

      void writeVarInt(int value) {
         BotConnection.writeVarInt(this.bytes, value);
      }

      void writeLong(long value) {
         for (int i = 7; i >= 0; i--) {
            this.bytes.write((int) (value >> (8 * i)) & 0xFF);
         }
      }

      void writeDouble(double value) {
         this.writeLong(Double.doubleToLongBits(value));
      }

      void writeFloat(float value) {
         long bits = Float.floatToIntBits(value);
         for (int i = 3; i >= 0; i--) {
            this.bytes.write((int) (bits >> (8 * i)) & 0xFF);
         }
      }

      void writeUnsignedShort(int value) {
         this.bytes.write((value >> 8) & 0xFF);
         this.bytes.write(value & 0xFF);
      }

      void writeUnsignedByte(int value) {
         this.bytes.write(value & 0xFF);
      }

      void writeByte(int value) {
         this.bytes.write(value & 0xFF);
      }

      void writeBoolean(boolean value) {
         this.bytes.write(value ? 1 : 0);
      }

      void writeString(String value) {
         byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
         this.writeVarInt(bytes.length);
         this.bytes.write(bytes, 0, bytes.length);
      }

      void writeBytes(byte[] value) {
         this.bytes.write(value, 0, value.length);
      }

      byte[] toByteArray() {
         return this.bytes.toByteArray();
      }
   }

   private static final class In {
      final byte[] data;
      int pos;

      In(byte[] data) {
         this.data = data;
      }

      int readVarInt() {
         int value = 0;
         for (int i = 0; i < 5; i++) {
            int b = this.data[this.pos++] & 0xFF;
            value |= (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) {
               return value;
            }
         }
         throw new IllegalStateException("VarInt слишком большой");
      }

      long readLong() {
         long value = 0L;
         for (int i = 7; i >= 0; i--) {
            value = (value << 8) | (this.data[this.pos++] & 0xFFL);
         }
         return value;
      }

      int readInt() {
         int value = (this.data[this.pos] & 0xFF) << 24 | (this.data[this.pos + 1] & 0xFF) << 16
            | (this.data[this.pos + 2] & 0xFF) << 8 | (this.data[this.pos + 3] & 0xFF);
         this.pos += 4;
         return value;
      }

      double readDouble() {
         return Double.longBitsToDouble(this.readLong());
      }

      float readFloat() {
         return Float.intBitsToFloat(bits32());
      }

      int bits32() {
         int value = (this.data[this.pos] & 0xFF) << 24 | (this.data[this.pos + 1] & 0xFF) << 16
            | (this.data[this.pos + 2] & 0xFF) << 8 | (this.data[this.pos + 3] & 0xFF);
         this.pos += 4;
         return value;
      }

      boolean readBoolean() {
         return this.data[this.pos++] != 0;
      }

      String readString() {
         int len = this.readVarInt();
         if (len < 0 || this.pos + len > this.data.length) {
            throw new IllegalStateException("Строка вне границ пакета");
         }
         String s = new String(this.data, this.pos, len, StandardCharsets.UTF_8);
         this.pos += len;
         return s;
      }

      UUID readUuid() {
         if (this.pos + 16 > this.data.length) {
            throw new IllegalStateException("UUID вне границ пакета");
         }
         long msb = 0L;
         long lsb = 0L;
         for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (this.data[this.pos++] & 0xFFL);
         }
         for (int i = 0; i < 8; i++) {
            lsb = (lsb << 8) | (this.data[this.pos++] & 0xFFL);
         }
         return new UUID(msb, lsb);
      }

      byte[] readRemaining() {
         int len = this.data.length - this.pos;
         byte[] out = new byte[len];
         System.arraycopy(this.data, this.pos, out, 0, len);
         this.pos = this.data.length;
         return out;
      }
   }
}