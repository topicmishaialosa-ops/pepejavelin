package tech.huihui.utility.os;

public final class OperatingSystem {
    private static final boolean IS_WINDOWS = isWindows();
    private static final boolean IS_MACOS = isMacOS();
    
    public static boolean isWindows() {
        return isWindowsFallback() || isWindowsRaw();
    }
    
    public static boolean isMacOS() {
        return isMacOSFallback() || isMacOSRaw();
    }
    
    public static String getSeparator() {
        return IS_WINDOWS ? "\\" : "/";
    }
    
    public static String getOSName() {
        return System.getProperty("os.name");
    }
    
    private static boolean isWindowsFallback() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows") || osName.contains("win") || osName.contains("microsoft");
    }
    
    private static boolean isWindowsRaw() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
    
    private static boolean isMacOSFallback() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") || osName.contains("os x");
    }
    
    private static boolean isMacOSRaw() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac os") || System.getProperty("os.name").toLowerCase().startsWith("darwin");
    }
}
