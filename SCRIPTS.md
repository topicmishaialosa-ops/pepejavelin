# Script System

Система скриптов для HuihuiClient.

## Команды

- `.script <lang> load <name>` — загрузить скрипт
- `.script <lang> unload <name>` — выгрузить скрипт
- `.script <lang> reload <name>` — перезагрузить скрипт
- `.script <lang> list` — показать список загруженных скриптов этого языка
- `.script list` — показать все загруженные скрипты
- `.script dir` — открыть папку со скриптами
- `.script <lang> dir` — открыть папку скриптов конкретного языка

## Поддерживаемые языки

### Java

Скрипты на Java компилируются рантаймом. Класс должен наследовать `tech.huihui.client.script.api.Script`.

```java
package script;

import tech.huihui.client.script.api.Script;

public class MyScript extends Script {
    @Override
    public void onLoad() {
        api.print("Скрипт загружен!");
    }

    @Override
    public void onUnload() {
        api.print("Скрипт выгружен!");
    }

    public void onTick() {
        api.print("Tick!");
    }
}
```

### Lua

Скрипты на Lua интерпретируются через Luaj.

> ⚠️ **ВАЖНО про вызов методов Java-объектов.** В luaj методы Java-объектов
> (`api`, `ctx` и т.д.) вызываются **через двоеточие**: `api:method(...)`, `ctx:method(...)`.
> Запись через точку (`api.method(...)`, `ctx.text(...)`) НЕ работает — luaj воспринимает
> первый аргумент как `self` и падает с `bad argument: userdata expected`.
> (Свойства/геттеры тоже через двоеточие: `ctx:getX()`.)

```lua
api:onTick(function()
    api:print("FPS: " .. api:mc():getCurrentFps())
end)

api:onRender2D(function(ctx)
    ctx:text("Hello!", 10, 10, 0xffffff)
end)
```

### JSON

Декларативная конфигурация модулей и HUD-виджетов.

```json
{
  "modules": [
    {
      "name": "Test Module",
      "category": "COMBAT",
      "key": 1,
      "onEnable": "enable AutoCrystal",
      "onTick": "chat tick"
    }
  ],
  "hud": [
    {
      "type": "text",
      "text": "FPS: {fps}",
      "x": 10,
      "y": 10,
      "color": 0xff00ff00
    }
  ]
}
```

## API

### RenderContext2D

Обычные примитивы:
- `ctx.rect(x, y, w, h, color)` — прямоугольник
- `ctx.outlineRect(x, y, w, h, thickness, color)` — контур
- `ctx.text(t, x, y, color)` / `ctx.text(t, x, y, color, shadow)` — текст ( vanilla-шрифт)
- `ctx.textWidth(t)` / `ctx.textHeight()` — размеры текста
- `ctx.line(x1, y1, x2, y2, thickness, color)` — линия
- `ctx.circle(x, y, r, color)` — круг

**Кастомный рендер клиента** (тот же, что в HUD самого клиента — Blur, RoundedRect, шрифты, тема):
- `ctx.custom()` — вернуть `CustomDrawContext` (полный доступ к `DrawUtil`, `drawText(Font,...)` и т.д.)
- `ctx.roundedRect(x, y, w, h, radius, color)` — закруглённый прямоугольник
- `ctx.roundedOutline(x, y, w, h, thickness, radius, color)` — закруглённая рамка
- `ctx.squircle(x, y, w, h, squirt, radius, color)` — сквёркл
- `ctx.blur(x, y, w, h, radius, color)` — размытие фона
- `ctx.shadow(x, y, w, h, softness, radius, color)` — мягкая тень
- `ctx.text(t, x, y, color, size)` — текст клиентским шрифтом `Fonts.REGULAR`
- `ctx.textGlow(t, x, y, color, size)` — текст с glow
- `ctx.theme()` — текущая тема (`Theme`), `ctx.color(int)` — `int` → `ColorRGBA`
- `ctx.matrices()` — `MatrixStack` кадра

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

### RenderContext3D

- `ctx.line3D(a, b, color)` — рисует 3D линию между двумя точками
- `ctx.box(min, max, color)` — рисует 3D куб

### Модули

- `api.module(name).category(cat).key(k).onEnable(fn).onDisable(fn).onTick(fn).setting(s).register()` — создаёт кастомный модуль

### HUD-модули

Полноценные переключаемые HUD-элементы (видны в кликГУИ, включаются/выключаются как модуль).

- `api.hud(name).pos(x,y).size(w,h).category(cat).key(k).onRender(ctx -> {...}).register()`
- В `onRender` приходит `HudRenderContext` с методами:
  - `ctx.getX()`, `ctx.getY()` — координаты верхнего левого угла элемента
  - `ctx.getWidth()`, `ctx.getHeight()` — размеры элемента
  - Все методы `RenderContext2D` (text, rect, line, circle, ...)

### Действия

- `api.print(msg)` — сообщение в чат
- `api.jump()` — прыжок
- `api.chat(msg)` — чат
- `api.sendCommand(cmd)` — команда
- `api.enableModule(name)` — включить модуль
- `api.disableModule(name)` — выключить модуль
- `api.toggleModule(name)` — переключить модуль
- `api.getModule(name)` — получить модуль

### Слоты инвентаря

Удобные методы для клика по слотам из скриптов:

- `api.clickSlot(slot)` — левый клик по слоту
- `api.clickSlot(slot, button)` — `button`: 0 — левый, 1 — правый
- `api.clickSlot(slot, button, action)` — `action`: `"pickup"` (по умолч.), `"quick_move"` (shift), `"swap"` (`button` = слот хотбара), `"clone"` (creative), `"throw"`/`"drop"` (выброс), `"pickup_all"`
- `api.dropSlot(slot)` — выбросить предмет из слота
- `api.quickMoveSlot(slot)` — shift-клик (быстро переместить)
- `api.selectHotbar(index)` — выбрать слот хотбара 0..8 (как цифра на клавиатуре)

Работает с текущим контейнером; если экран не открыт — с инвентарём игрока.

- `api.clickSlotInventory(slot)` — клик слота **своего инвентаря** (работает даже при открытом сундуке/печке)
- `api.clickSlotInventory(slot, button)` / `api.clickSlotInventory(slot, button, action)` — то же с кнопкой/действием
- Нумерация слотов как в Inventory: `0..8` — хотбар, `9..35` — основной инвентарь, `36..39` — броня

Пример (Lua):
```lua
api.clickSlot(15, 1, "throw")       -- выбросить предмет из слота 15 открытого контейнера (пкм)
api.clickSlotInventory(0, 1, "throw") -- выбросить предмет из 1-го слота хотбара своего инвентаря
api.selectHotbar(0)                 -- выбрать первый слот хотбара
```

### Lua

- `api.registerHud(name, category, key, x, y, w, h, { onRender = function(ctx) ... end })` — HUD-модуль
- `api.registerModule(name, category, key, { onEnable=..., onDisable=..., onTick=... })` — модуль
- `api.print(msg)` — вывод
