package com.kobi401.browser.utils.debug;

public class Debugger {
    //TODO: change this to use System.property
    private static boolean debugEnabled = false;  //set this to true when you want to enable debug output

    public static void enableDebugging(boolean enable) {
        debugEnabled = enable;
    }

    public static void println(String message) {
        if (debugEnabled) {
            System.out.println(message);
        }
    }
}

