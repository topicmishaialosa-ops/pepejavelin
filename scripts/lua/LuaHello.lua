api:onTick(function()
    api:print("FPS: " .. api:mc():getCurrentFps())
end)

api:onKey(function(key)
    if key == 1 then  -- Left click
        api:chat("Left clicked!")
    end
end)

-- HUD-модуль (переключается в кликГУИ по названию "Lua FPS")
api:registerHud("Lua FPS", "RENDER", -1, 10, 40, 120, 20, {
    onRender = function(ctx)
        ctx:text("FPS: " .. api:mc():getCurrentFps(), ctx:getX(), ctx:getY(), 0xff00ff00)
    end
})
