package script;

import tech.huihui.client.script.api.Script;

public class CoalMiner extends Script {

    private int ticks = 0;
    private int seconds = 0;
    private boolean isRunning = false;
    private int cooldown = 0;

    @Override
    public void onLoad() {
        // api.print -> MessageUtil.displayInfo (уведомление, а не сообщение в чат)
        api.print("Coal Miner скрипт загружен!");

        api.module("Coal Miner")
            .category("MISC")
            .key(-1)
            .onEnable(() -> {
                isRunning = true;
                ticks = 0;
                seconds = 0;
                api.print("Начинаем добычу угля!");
            })
            .onDisable(() -> {
                isRunning = false;
                api.print("Остановка добычи угля. Всего: " + seconds + " сек");
            })
            .onTick(() -> {
                if (!isRunning) return;

                ticks++;
                cooldown--;

                if (ticks % 20 == 0) {
                    seconds++;
                }

                if (ticks % 100 == 0 && cooldown <= 0) {
                    // пишем в чат команду с префиксом '#'
                    api.chat("#mine coal_ore");
                    api.print("Добываю уголь... (" + seconds + " сек)");
                    cooldown = 20;
                }
            })
            .register();

        api.onRender2D((ctx) -> {
            if (!isRunning) return;

            double x = 10;
            double y = 100;
            double w = 210;
            double h = 48;

            // фон с размытием (кастомный рендер клиента)
            ctx.blur(x, y, w, h, 14, 0xAA0b0b0f);
            // полупрозрачная подложка + мягкая тень
            ctx.shadow(x, y, w, h, 12, 12, 0x55000000);
            ctx.roundedRect(x, y, w, h, 12, 0x40202028);
            ctx.roundedOutline(x, y, w, h, 1.5, 12, 0xff4cc2ff);

            // заголовок клиентским шрифтом с glow
            ctx.textGlow("Coal Miner", x + 12, y + 8, 0xffffffff, 14);

            // таймер обычным клиентским шрифтом
            String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
            ctx.text(time, x + 12, y + 28, 0xff9fe6ff, 12);

            // прогресс-бар
            double barW = w - 24;
            double progress = (ticks % 100) / 100.0;
            double barY = y + h - 12;
            ctx.roundedRect(x + 12, barY, barW, 6, 3, 0x55333333);
            ctx.roundedRect(x + 12, barY, barW * progress, 6, 3, 0xff4cc2ff);
        });
    }

    @Override
    public void onUnload() {
        isRunning = false;
        api.print("Coal Miner выгружен!");
    }
}
