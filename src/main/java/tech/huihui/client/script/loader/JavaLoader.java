package tech.huihui.client.script.loader;

import tech.huihui.client.script.LoadedScript;
import tech.huihui.client.script.api.Script;
import tech.huihui.client.script.api.ScriptAPI;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Загрузчик Java-скриптов: компилирует .java в рантайме и инстанцирует класс, наследующий {@link Script}.
 */
public class JavaLoader {

    public static LoadedScript load(String name, File file, ScriptAPI api) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Нет Java-компилятора (нужен JDK, а не просто JRE)");
        }
        File out = Files.createTempDirectory("pepe_script_").toFile();
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diag, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(file);
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(out.getAbsolutePath());
            options.add("-classpath");
            options.add(buildClasspath());
            Boolean ok = compiler.getTask(null, fm, diag, options, null, units).call();
            if (ok == null || !ok) {
                StringBuilder sb = new StringBuilder("Ошибка компиляции");
                for (Diagnostic<?> d : diag.getDiagnostics()) sb.append("\n").append(d);
                throw new RuntimeException(sb.toString());
            }
        }
        URLClassLoader cl = new URLClassLoader(new URL[]{out.toURL()}, JavaLoader.class.getClassLoader());
        Class<?> scriptClass = findScriptClass(cl, out);
        if (scriptClass == null) {
            throw new RuntimeException("Класс скрипта не найден (должен наследовать Script)");
        }
        Object inst = scriptClass.getDeclaredConstructor().newInstance();
        if (!(inst instanceof Script)) {
            throw new RuntimeException("Скрипт должен наследовать tech.huihui.client.script.api.Script");
        }
        ((Script) inst).api = api;
        ((Script) inst).onLoad();
        final Object finst = inst;
        return new LoadedScript(name, "java", file, api, finst, a -> ((Script) finst).onUnload());
    }

    private static Class<?> findScriptClass(URLClassLoader cl, File out) throws Exception {
        List<File> classFiles = new ArrayList<>();
        collect(out, classFiles);
        for (File f : classFiles) {
            String path = f.getAbsolutePath().substring(out.getAbsolutePath().length() + 1)
                    .replace(File.separatorChar, '.').replace(".class", "");
            Class<?> c = cl.loadClass(path);
            if (Script.class.isAssignableFrom(c)) return c;
        }
        return null;
    }

    private static void collect(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().endsWith(".class")) out.add(f);
        }
    }

    private static String buildClasspath() {
        StringBuilder cp = new StringBuilder();
        String jcp = System.getProperty("java.class.path");
        if (jcp != null && !jcp.isEmpty()) cp.append(jcp);

        // 1) Обходим все класслоадеры (включая родительские) и собираем их URL.
        //    Мод-класслоадеры (Fabric/Quilt) часто не являются URLClassLoader,
        //    поэтому пробуем рефлексией несколько известных методов.
        ClassLoader cl = JavaLoader.class.getClassLoader();
        while (cl != null) {
            collectLoaderUrls(cl, cp);
            cl = cl.getParent();
        }

        // 2) Гарантированно добавляем jar'ы, в которых лежат ключевые классы
        //    (клиент, майнкрафт, fabric, brigadier) — берём их CodeSource.
        addCodeSource(ScriptAPI.class, cp);
        addCodeSource(net.minecraft.client.MinecraftClient.class, cp);
        try { addCodeSource(net.fabricmc.api.ModInitializer.class, cp); } catch (Throwable ignored) { }
        try { addCodeSource(com.mojang.brigadier.Command.class, cp); } catch (Throwable ignored) { }

        return cp.toString();
    }

    private static void collectLoaderUrls(ClassLoader cl, StringBuilder cp) {
        if (cl instanceof URLClassLoader) {
            for (URL u : ((URLClassLoader) cl).getURLs()) append(cp, u);
        }
        // мод-загрузчики могут отдавать URL через один из этих методов
        for (String m : new String[]{"getURLs", "getClassPathURLs", "getClassPath"}) {
            try {
                java.lang.reflect.Method meth = cl.getClass().getMethod(m);
                Object res = meth.invoke(cl);
                if (res instanceof URL[]) {
                    for (URL u : (URL[]) res) append(cp, u);
                } else if (res instanceof java.util.Collection) {
                    for (Object o : (java.util.Collection<?>) res) {
                        if (o instanceof URL) append(cp, (URL) o);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void addCodeSource(Class<?> c, StringBuilder cp) {
        try {
            if (c != null && c.getProtectionDomain() != null
                    && c.getProtectionDomain().getCodeSource() != null
                    && c.getProtectionDomain().getCodeSource().getLocation() != null) {
                append(cp, c.getProtectionDomain().getCodeSource().getLocation());
            }
        } catch (Exception ignored) {
        }
    }

    private static void append(StringBuilder cp, URL u) {
        if (u == null) return;
        String p;
        try {
            p = new File(u.toURI()).getAbsolutePath();
        } catch (Exception e) {
            p = u.getFile();
        }
        if (p == null || p.isEmpty()) return;
        if (cp.length() > 0 && cp.charAt(cp.length() - 1) != File.pathSeparatorChar) {
            cp.append(File.pathSeparator);
        }
        cp.append(p);
    }
}
