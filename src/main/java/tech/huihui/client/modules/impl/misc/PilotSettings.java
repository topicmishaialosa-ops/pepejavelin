package tech.huihui.client.modules.impl.misc;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "PilotSettings",
        description = "Настройки команды .pilot; включать модуль не нужно",
        category = Category.MISC
)
public final class PilotSettings extends Module {
    public static final PilotSettings INSTANCE = new PilotSettings();

    private final NumberSetting fireworksPerSecond = new NumberSetting(
            "Фейерверков в секунду",
            0.083F,
            0.02F,
            1.0F,
            0.001F,
            "0.083 = один фейерверк примерно раз в 12 секунд"
    );
    private final NumberSetting flightHeight = new NumberSetting(
            "Высота полета Y",
            150.0F,
            -64.0F,
            320.0F,
            1.0F,
            "Крейсерская высота автопилота"
    );
    private final BooleanSetting avoidObstacles = new BooleanSetting(
            "Облетать препятствия",
            "Автопилот будет подниматься, если впереди есть блоки",
            true
    );

    private PilotSettings() {
    }

    public float getFireworksPerSecond() {
        return fireworksPerSecond.getCurrent();
    }

    public double getFlightHeight() {
        return flightHeight.getCurrent();
    }

    public boolean isAvoidObstacles() {
        return avoidObstacles.isEnabled();
    }
}
