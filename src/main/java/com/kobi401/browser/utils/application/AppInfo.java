package com.kobi401.browser.utils.application;

import java.util.Locale;

public class AppInfo {

    private final String version;
    private final String buildType;
    private final String osName;
    private String build;

    public AppInfo(String version, String build, String buildType) {
        this.version = version;
        this.build = build;
        this.buildType = buildType;
        this.osName = System.getProperty("os.name");
    }

    public String getVersion() {
        return version;
    }

    public String getBuild() { return build; }

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
        String version = "2.1.4";
        String build = "2025.04.05";
        String buildType = System.getProperty("build.type", "Stable").toUpperCase(Locale.ROOT);
        return new AppInfo(version, build, buildType);
    }
}
