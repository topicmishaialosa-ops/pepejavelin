-- HudDemo: пример HUD с кастомным рендером клиента (blur, rounded, glow)
-- Загружается командой:  .script lua load HudDemo
-- ВАЖНО: методы Java-объектов (api, ctx) в luaj вызываются ЧЕРЕЗ ДВОЕТОЧИЕ (api:method / ctx:method),
-- а не через точку (api.method НЕ работает — даёт "bad argument: userdata expected").

api:registerHud("HudDemo", "RENDER", -1, 250, 200, 180, 70, {
    onRender = function(ctx)
        local x = ctx:getX()
        local y = ctx:getY()
        local w = 180
        local h = 70

        -- размытие фона (тот же DrawUtil, что и в HUD клиента)
        ctx:blur(x, y, w, h, 14, 0xAA0b0b0f)
        -- мягкая тень
        ctx:shadow(x, y, w, h, 10, 12, 0x55000000)
        -- закруглённая рамка
        ctx:roundedOutline(x, y, w, h, 1.5, 10, 0xff4cc2ff)

        -- заголовок клиентским шрифтом с glow
        ctx:textGlow("Huihui Scripts", x + 10, y + 10, 0xffffffff, 14)

        -- строка статуса обычным текстом
        ctx:text("custom render OK", x + 10, y + 34, 0xff9fe6ff, 11)

        -- маленький squircle-индикатор
        ctx:squircle(x + w - 34, y + 30, 24, 24, 6, 12, 0xff4cc2ff)
    end
})
