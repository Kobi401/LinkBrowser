package com.kobi401.browser.utils;

import java.util.Locale;

public class AppInfo {

    private final String version;
    private final String buildType;
    private final String osName;

    public AppInfo(String version, String buildType) {
        this.version = version;
        this.buildType = buildType;
        this.osName = System.getProperty("os.name");
    }

    public String getVersion() {
        return version;
    }

    public String getBuildType() {
        return buildType;
    }

    public String getOsName() {
        return osName;
    }

    public String getFormattedVersion() {
        return version + " - " + buildType + " (" + osName + ")";
    }

    public static AppInfo createDefaultAppInfo() {
        String version = "2.1.1";
        String buildType = System.getProperty("build.type", "STABLE").toUpperCase(Locale.ROOT);
        return new AppInfo(version, buildType);
    }
}
