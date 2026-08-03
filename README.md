# pepejavelin

Паста исходников **Javelin 1.21.4** для Fabric 1.21.4.

Этот проект — форк/копия исходного кода клиента Javelin (1.21.4) с доработками и новыми модулями.

## Сборка

```bash
./gradlew build
```

Готовый jar лежит в `build/libs/huihuiclient-0.1-recode.jar`.

## Структура

- `src/main/java/tech/huihui/client/modules/impl/` — модули клиента
  - `combat/` — боевые модули (Aura, AutoDuel, TargetStrafe, AntiElytraTarget и др.)
  - `misc/` — вспомогательные модули (AutoJoiner, Autofarm, CreeperFarm, ZarabotokReallyWorld и др.)
  - `render/` — рендер-модули (TargetHud, TargetESP, XRay и др.)
- `src/main/java/tech/huihui/base/` — базовая система (модули, команды, события)
- `src/main/java/tech/huihui/utility/` — утилиты и миксины

## Дисклеймер

Проект создан в образовательных целях. Используйте на свой страх и риск.
