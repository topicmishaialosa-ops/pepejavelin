package tech.huihui.client.script.api;

/**
 * Базовый класс для Java-скриптов. Пользовательский скрипт наследует его,
 * реализует onLoad()/onUnload() и использует поле {@link #api} для доступа к API.
 */
public abstract class Script {
    public ScriptAPI api;

    public abstract void onLoad();

    public void onUnload() {
    }
}
