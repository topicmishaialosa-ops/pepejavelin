# Система скриптов HuihuiClient

Скрипты позволяют расширять функциональность клиента без пересборки мода: писать
свои модули, HUD-виджеты, автоматизацию и обработчики событий на **Java**, **Lua**
или декларативно на **JSON**.

---

## 1. Расположение и структура файлов

Скрипты лежат в папке:

```
%userprofile%/.minecraft/Huihui/scripts      (Windows)
~/.minecraft/Huihui/scripts                  (Linux/macOS)
```

При первом запуске и через команду `.script dir` папка создаётся. Рекомендуемая
структура (по языкам):

```
scripts/
├── java/        # .java файлы
├── lua/         # .lua файлы
├── json/        # .json файлы
└── examples/    # общие примеры (ищутся тоже)
```

Имя скрипта при загрузке — это имя файла **без расширения**. Поиск файла идёт в
порядке: `scripts/<lang>/<name>.<lang>` → `scripts/<name>.<lang>` →
`scripts/examples/<name>.<lang>`.

---

## 2. Команды

| Команда | Назначение |
| --- | --- |
| `.script <lang> load <name>` | загрузить скрипт |
| `.script <lang> unload <name>` | выгрузить скрипт |
| `.script <lang> reload <name>` | перезагрузить (unload + load) |
| `.script <lang> list` | список загруженных скриптов этого языка |
| `.script list` | все загруженные скрипты |
| `.script dir` | открыть корневую папку скриптов |
| `.script <lang> dir` | открыть папку скриптов языка |

`<lang>` ∈ `java` | `lua` | `json`.

При загрузке автоматически вызываются колбэки `onEnable` (и `onLoad` для Java/Lua).
При выгрузке — всё, что зарегистрировал скрипт (модули, HUD, колбэки), удаляется
автоматически; плюс вызываются `onUnload` / `onDisable`.

---

## 3. Общий цикл жизни скрипта

- Для каждого загруженного скрипта создаётся свой экземпляр `ScriptAPI`.
- Все зарегистрированные модули/HUD и подписки на события привязаны к этому
  экземпляру и очищаются при выгрузке (`ScriptAPI.dispose()`). Дополнительно
  чистить вручную ничего не нужно.
- Состояние (поля класса в Java, глобальные переменные в Lua) живёт, пока скрипт
  загружен. `reload` создаёт заново — старое состояние теряется.

---

## 4. Java

Скрипт компилируется **в рантайме** через `javax.tools.JavaCompiler`. Поэтому
нужен **JDK** (а не просто JRE) — иначе при загрузке будет ошибка
`Нет Java-компилятора`.

Требования к классу:

- пакет не обязателен, но удобно `package script;`;
- класс наследует `tech.huihui.client.script.api.Script`;
- реализует `onLoad()` (обязательно) и опционально `onUnload()`;
- доступ к API — через поле `api` (тип `ScriptAPI`), оно проставляется загрузчиком
  **до** вызова `onLoad`.

```java
package script;

import tech.huihui.client.script.api.Script;
import tech.huihui.client.script.api.HudRenderContext;

public class HelloWorld extends Script {

    private int ticks = 0;

    @Override
    public void onLoad() {
        api.print("HelloWorld загружен!");

        // Обычный модуль
        api.module("HW Module")
            .category("MISC")
            .key(-1)
            .onEnable(() -> api.print("HW Module вкл"))
            .onDisable(() -> api.print("HW Module выкл"))
            .onTick(() -> { if (++ticks % 20 == 0) api.print("tick " + ticks); })
            .register();

        // HUD-модуль (рисуется только когда включён)
        api.hud("HW HUD")
            .pos(10, 70).size(120, 20)
            .category("RENDER").key(-1)
            .onRender((HudRenderContext ctx) ->
                ctx.text("Hello HUD!", ctx.getX(), ctx.getY(), 0xff00ff00))
            .register();
    }

    @Override
    public void onUnload() {
        api.print("HelloWorld выгружен!");
    }
}
```

Колбэки принимают параметры явно:

```java
api.onRender2D((RenderContext2D ctx) -> { /* ... */ });
api.onRender3D((RenderContext3D ctx) -> { /* ... */ });
api.onKey((Integer key) -> { /* ... */ });
api.onChatSend((String msg) -> { /* ... */ });
api.onChatReceive((String msg) -> { /* ... */ });
```

---

## 5. Lua

Интерпретируется через **luaj**. Глобально доступны:

- `api` — мост к `ScriptAPI` (`LuaApiBridge`);
- `print(...)` — дублирует вывод в уведомление клиента (как `api.print`).

### ⚠️ Вызов методов Java-объектов

В luaj методы Java-объектов (`api`, `ctx` и т.д.) вызываются **через двоеточие**:
`api:method(...)`, `ctx:method(...)`. Запись через точку (`api.method(...)`,
`ctx.text(...)`) **НЕ работает** — luaj воспринимает первый аргумент как `self` и
падает с `bad argument: userdata expected`. Свойства/геттеры тоже через двоеточие:
`ctx:getX()`.

Необязательные точки входа (функции в глобальном scope):

- `onLoad()` — вызывается после выполнения скрипта;
- `onUnload()` — при выгрузке.

```lua
api:onTick(function()
    api:print("FPS: " .. api:mc():getCurrentFps())
end)

api:onKey(function(key)
    if key == 1 then  -- Left click (GLFW_MOUSE_BUTTON_1)
        api:chat("Left clicked!")
    end
end)

-- HUD-модуль (переключается в кликГУИ по названию "Lua FPS")
api:registerHud("Lua FPS", "RENDER", -1, 10, 40, 120, 20, {
    onRender = function(ctx)
        ctx:text("FPS: " .. api:mc():getCurrentFps(), ctx:getX(), ctx:getY(), 0xff00ff00)
    end
})
```

Колбэки: `api:onTick(fn)`, `api:onRender2D(fn)`, `api:onRender3D(fn)`,
`api:onKey(fn)`, `api:onChatSend(fn)`, `api:onChatReceive(fn)`, `api:onEnable(fn)`,
`api:onDisable(fn)`. В `onRender2D`/`onRender3D` первый аргумент — контекст;
в `onKey` — код клавиши; в `onChatSend`/`onChatReceive` — строка сообщения.

---

## 6. JSON (декларативно)

Описывает модули и HUD-виджеты без кода. Файл — объект с ключами `modules` и/или
`hud`.

```json
{
  "modules": [
    {
      "name": "Test Module",
      "category": "COMBAT",
      "key": 1,
      "onEnable": "enable Blocker",
      "onDisable": "disable Blocker",
      "onTick": "chat test module tick"
    }
  ],
  "hud": [
    {
      "name": "JSON FPS",
      "type": "text",
      "text": "FPS: {fps}",
      "x": 10,
      "y": 10,
      "width": 120,
      "height": 20,
      "color": 0xff00ff00,
      "category": "RENDER",
      "key": -1
    }
  ]
}
```

### Модули (JSON)

Поля модуля: `name` (обяз.), `category`, `key`, `onEnable`, `onDisable`, `onTick`.
Значения `on*` — **строки-действия** (см. ниже).

### HUD (JSON)

Поля виджета: `name`, `type`, `text`, `x`, `y`, `width`, `height`, `color`,
`category`, `key`. Поддерживаемые `type`:

- `text` — выводит `text` с подстановкой плейсхолдеров;
- `coords` — выводит координаты игрока (`X: .. Y: .. Z: ..`).

Плейсхолдеры в `text`: `{x}`, `{y}`, `{z}`, `{fps}`, `{user}`, `{ping}`.

### Строки-действия (action)

Используются в `onEnable`/`onDisable`/`onTick` JSON-модулей:

| Префикс | Что делает |
| --- | --- |
| `chat <текст>` | отправляет `<текст>` в чат |
| `command <команда>` | выполняет команду сервера (без `/`) |
| `enable <модуль>` | включает модуль клиента по имени |
| `disable <модуль>` | выключает модуль клиента по имени |

Например, `onEnable: "enable Blocker"` включит модуль `Blocker` при активации
скрипт-модуля.

---

## 7. Категории модулей

`category` задаётся строкой (регистр не важен). Допустимые значения:

`COMBAT`, `MOVEMENT`, `PLAYER`, `RENDER`, `MISC`, `COSMETICS`, `THEMES`.

Если категория не указана или не распознана — используется `MISC` (для HUD —
`RENDER`).

---

## 8. API: `ScriptAPI` (общий)

Доступен как `api` (Java) / `api` (Lua).

### Колбэки событий

| Метод | Когда | Аргумент |
| --- | --- | --- |
| `onTick(Runnable)` | каждый тик | — |
| `onRender2D(Consumer<RenderContext2D>)` | каждый кадр (2D) | `RenderContext2D` |
| `onRender3D(Consumer<RenderContext3D>)` | каждый кадр (3D) | `RenderContext3D` |
| `onKey(Consumer<Integer>)` | нажатие клавиши/кнопки мыши | код клавиши (GLFW) |
| `onChatSend(Consumer<String>)` | отправка сообщения в чат | текст |
| `onChatReceive(Consumer<String>)` | входящее сообщение чата | текст |
| `onEnable(Runnable)` | включение скрипта | — |
| `onDisable(Runnable)` | выключение скрипта | — |

В Lua те же методы, но аргумент — Lua-функция: `api:onTick(fn)`.

### Действия игрока / клиента

| Метод | Описание |
| --- | --- |
| `mc()` | `MinecraftClient.getInstance()` (доступ к майнкрафту) |
| `print(msg)` | уведомление (не в чат) |
| `jump()` | прыжок игрока |
| `chat(msg)` | отправить сообщение в чат |
| `sendCommand(cmd)` | отправить команду сервера (без `/`) |
| `enableModule(name)` | включить модуль клиента |
| `disableModule(name)` | выключить модуль клиента |
| `toggleModule(name)` | переключить модуль клиента |
| `getModule(name)` | получить объект модуля (или `null`) |

### Слоты инвентаря

Работают с **текущим контейнером** (`currentScreenHandler`); если экран не открыт —
с инвентарём игрока.

- `clickSlot(slot)` — левый клик (button 0, pickup)
- `clickSlot(slot, button)` — `button`: 0 — левый, 1 — правый
- `clickSlot(slot, button, action)` — `action`: `"pickup"` (по умолч.),
  `"quick_move"` (shift), `"swap"` (`button` = слот хотбара), `"clone"` (creative),
  `"throw"`/`"drop"` (выброс), `"pickup_all"`
- `dropSlot(slot)` — выбросить предмет (ПКМ + throw)
- `quickMoveSlot(slot)` — shift-клик (быстро переместить)
- `selectHotbar(index)` — выбрать слот хотбара 0..8 (как цифра на клавиатуре)

Клик по **собственному** инвентарю игрока, даже если открыт другой экран
(сундук/печка):

- `clickSlotInventory(slot)` / `clickSlotInventory(slot, button)` /
  `clickSlotInventory(slot, button, action)`

Нумерация слотов инвентаря: `0..8` — хотбар, `9..35` — основной инвентарь,
`36..39` — броня (offhand через этот API недоступен).

Пример (Lua):

```lua
api.clickSlot(15, 1, "throw")          -- выбросить предмет из слота 15 открытого контейнера (ПКМ)
api.clickSlotInventory(0, 1, "throw")  -- выбросить предмет из 1-го слота хотбара своего инвентаря
api.selectHotbar(0)                    -- выбрать первый слот хотбара
```

### Создание кастомных модулей

```java
api.module(name)
   .category(cat)        // строка категории
   .key(k)               // клавиша (GLFW-код) или -1
   .onEnable(fn)
   .onDisable(fn)
   .onTick(fn)
   .setting(s)           // опционально, один или несколько Setting
   .register();
```

Lua-аналог:

```lua
api.registerModule("My Mod", "MISC", -1, {
    onEnable  = function() api.print("on") end,
    onDisable = function() api.print("off") end,
    onTick    = function() api.print("tick") end
})
```

### Создание HUD-модулей

Полноценные переключаемые HUD-элементы (видны в кликГУИ, включаются/выключаются как
модуль).

```java
api.hud(name)
   .pos(x, y)
   .size(w, h)
   .category(cat)
   .key(k)
   .onRender((HudRenderContext ctx) -> { /* ... */ })
   .register();
```

Lua-аналог:

```lua
api.registerHud("My HUD", "RENDER", -1, x, y, w, h, {
    onRender = function(ctx) ... end
})
```

---

## 9. API: `RenderContext2D`

Передаётся в `onRender2D` и в `onRender` HUD-модулей. Обёртка над клиентским
`CustomDrawContext` — доступен «тот самый» кастомный рендер (Blur, RoundedRect,
шрифты, тема).

### Служебное

- `raw()` — исходный vanilla `DrawContext`
- `custom()` — клиентский `CustomDrawContext` (полный доступ к `DrawUtil`,
  `drawText(Font, ...)` и т.д.)
- `matrices()` — `MatrixStack` текущего кадра
- `mc()` — `MinecraftClient`
- `theme()` — текущая тема (`Theme`)
- `color(int)` — `int` (0xAARRGGBB) → `ColorRGBA`

### Базовые примитивы (vanilla-шрифт)

- `text(t, x, y, color)`
- `text(t, x, y, color, shadow)` — `shadow`: `true`/`false`
- `textWidth(t)` / `textHeight()` — размеры текста
- `rect(x, y, w, h, color)` — прямоугольник
- `outlineRect(x, y, w, h, thickness, color)` — контур
- `line(x1, y1, x2, y2, thickness, color)` — линия
- `circle(x, y, r, color)` — круг

### Кастомный рендер клиента

- `roundedRect(x, y, w, h, radius, color)` — закруглённый прямоугольник
- `roundedOutline(x, y, w, h, thickness, radius, color)` — закруглённая рамка
- `squircle(x, y, w, h, squirt, radius, color)` — «squircle»
- `blur(x, y, w, h, radius, color)` — размытие фона
- `shadow(x, y, w, h, softness, radius, color)` — мягкая тень
- `text(t, x, y, color, size)` — текст клиентским шрифтом `Fonts.REGULAR`
- `textGlow(t, x, y, color, size)` — текст с glow

В Lua все методы вызываются через двоеточие: `ctx:text(...)`, `ctx:blur(...)`,
`ctx:roundedOutline(...)`, `ctx:getX()` и т.д.

Пример (Java):

```java
api.hud("BlurBox").pos(200, 200).size(160, 60).onRender(ctx -> {
    ctx.blur(ctx.getX(), ctx.getY(), 160, 60, 12, 0x88000000);
    ctx.roundedOutline(ctx.getX(), ctx.getY(), 160, 60, 1.5, 8, 0xffffaa00);
    ctx.text("Hello HUD", ctx.getX() + 8, ctx.getY() + 8, 0xffffffff, 12);
}).register();
```

Пример (Lua):

```lua
api:registerHud("BlurBox", "RENDER", -1, 200, 200, 160, 60, {
    onRender = function(ctx)
        ctx:blur(ctx:getX(), ctx:getY(), 160, 60, 12, 0x88000000)
        ctx:roundedOutline(ctx:getX(), ctx:getY(), 160, 60, 1.5, 8, 0xffffaa00)
        ctx:text("Hello HUD", ctx:getX() + 8, ctx:getY() + 8, 0xffffffff, 12)
    end
})
```

---

## 10. API: `RenderContext3D`

Передаётся в `onRender3D`. Координаты — мировые (`Vec3d`), автоматически
смещаются относительно камеры.

- `matrix()` — `MatrixStack`
- `partialTicks()` — доля тика кадра
- `mc()` — `MinecraftClient`
- `line3D(a, b, color)` — 3D-линия между `Vec3d a` и `Vec3d b`
- `box(min, max, color)` — 3D-куб между `Vec3d min` и `Vec3d max`

Пример (Java):

```java
api.onRender3D((ctx) -> {
    var p = ctx.mc().player;
    var a = p.getPos();
    var b = a.add(0, 50, 0);
    ctx.line3D(a, b, 0xffff0000);
    ctx.box(a, a.add(1, 1, 1), 0xff00ff00);
});
```

---

## 11. API: `HudRenderContext`

Наследует `RenderContext2D` и добавляет координаты/размеры самого HUD-элемента
(верхний левый угол):

- `getX()` — X верхнего левого угла
- `getY()` — Y верхнего левого угла
- `getWidth()` — ширина элемента
- `getHeight()` — высота элемента

Все остальные методы `RenderContext2D` (`text`, `rect`, `line`, `circle`,
`blur`, `roundedRect`, `squircle`, `shadow`, `textGlow` и т.д.) доступны.

---

## 12. Примеры из поставки

В репозитории:

- `scripts/examples/HelloWorld.java` — модуль + HUD
- `scripts/examples/CoalMiner.java` — модуль-таймер с кастомным HUD
- `scripts/examples/LuaHello.lua` — колбэки + HUD на Lua
- `scripts/examples/test.json` — JSON-модуль + HUD
- `scripts/lua/HudDemo.lua` — HUD с blur/rounded/glow (клиентский рендер)
- `scripts/lua/LuaHello.lua` — минимальный Lua-скрипт

Загрузка примеров (файлы ищутся и в `examples/`):

```
.script java load HelloWorld
.script java load CoalMiner
.script lua load LuaHello
.script lua load HudDemo
.script json load test
```

---

## 13. Советы и частые ошибки

- **Java требует JDK.** Если запущено под JRE, загрузка `.java` упадёт с
  `Нет Java-компилятора`.
- **Lua: только двоеточие.** `api.method(...)` не работает, нужно `api:method(...)`.
- **Цвета** — `int` в формате `0xAARRGGBB` (например `0xff00ff00` — зелёный).
- **Координаты HUD** задавайте относительно `ctx:getX()`/`ctx:getY()`, чтобы виджет
  корректно перетаскивался в кликГУИ.
- **Имя файла = имя скрипта.** Для `HelloWorld.java` загрузка — `.script java load HelloWorld`.
- **Пересекающиеся имена.** Если скрипт с таким именем уже загружен, повторный
  `load` выдаст «уже загружен» — сначала `unload`/`reload`.
- **Очистка автоматическая.** При `unload` удаляются все модули/HUD и отписки
  скрипта; `onUnload` используйте только для своей логики (сохранение состояния и т.п.).
- **Слоты инвентаря** через `clickSlot` бьют по *открытому* контейнеру; чтобы кликать
  строго по своему инвентарю при открытом сундуке — используйте `clickSlotInventory`.
- **События чата** (`onChatSend`/`onChatReceive`) получают уже готовый текст;
  `onChatSend` срабатывает до отправки — можно анализировать/перехватывать команды.

---

## 14. Встраивание в существующие модули

Скрипт может не только создавать свои модули, но и «встраиваться» в уже существующие
модули клиента: реагировать на их включение/выключение и читать/менять их настройки.
Это работает из любого языка (Java/Lua/JSON) через объект `api`.

### 14.1 Реакция на переключение модуля

- `api.onModuleToggle("KillAura", enabled -> {...})` — вызывается при каждом
  включении/выключении модуля; аргумент `enabled` (boolean).
- `api.onModuleEnable("KillAura", () -> {...})` — только при включении.
- `api.onModuleDisable("KillAura", () -> {...})` — только при выключении.

Все колбэки автоматически отписываются при выгрузке скрипта.

### 14.2 Чтение и изменение настроек модуля

Сначала узнайте имена настроек:
- `api.getModuleSettings("KillAura")` — список имён настроек (List<String>).
- `api.getModuleCategory("KillAura")` — категория (строка).
- `api.isModuleEnabled("KillAura")` — включён ли сейчас модуль.

Типизированный доступ (mod — имя модуля, setting — имя настройки):
- boolean:  `getModuleBoolean(mod, setting)` / `setModuleBoolean(mod, setting, v)`
- number:   `getModuleNumber(mod, setting)`  / `setModuleNumber(mod, setting, v)`  (float/double)
- string:   `getModuleString(mod, setting)`  / `setModuleString(mod, setting, v)`
- mode:     `getModuleMode(mod, setting)`    / `setModuleMode(mod, setting, v)`   (v — имя режима)
- color:    `getModuleColor(mod, setting)`   / `setModuleColor(mod, setting, v)`  (int RGBA)
- key:      `getModuleKey(mod, setting)`     / `setModuleKey(mod, setting, v)`    (int keycode)

Пример (Lua):

```lua
-- name: AuraSync
api.onModuleEnable("KillAura", function()
  api.setModuleMode("KillAura", "target", "Nearest")
  api.setModuleNumber("KillAura", "range", 4.5)
  api.print("KillAura настроен скриптом")
end)

api.onTick(function()
  if api.isModuleEnabled("KillAura") then
    api.setModuleNumber("KillAura", "aps", 12)
  end
end)
```

Пример (Java):

```java
// name: AuraSync
api.onModuleToggle("KillAura", enabled -> {
  if (enabled) {
    api.setModuleBoolean("KillAura", "rotate", true);
    api.print("KillAura вкл, rotate=true");
  }
});
```

JSON не поддерживает произвольный код, поэтому для логики `onModuleToggle`/`onTick`
используйте Java или Lua; JSON подходит для декларативных модулей/HUD и вызова
`api`-методов внутри полей `onEnable`/`onTick` (см. раздел 4).

---

## 15. Архитектура (кратко)

- `ScriptManager` — регистрирует `ScriptEventBridge`, управляет загрузкой/выгрузкой.
- `ScriptEventBridge` — перехватывает события клиента (tick, render2D/3D, key, chat)
  и раздаёт их всем `ScriptAPI`.
- `ScriptAPI` — один на скрипт, хранит колбэки и созданные модули, чистит при выгрузке.
- Загрузчики: `JavaLoader` (in-memory компиляция), `LuaLoader` (luaj, глобальные
  `api`/`print`), `JsonLoader` (декларативные модули/HUD).
