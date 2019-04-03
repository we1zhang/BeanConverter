package com.zw.beanconverter.utils;


/**
 * string util
 */
public class StringUtil {

    private StringUtil() {

    }

    public static String capitalize(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static String firstCharToLowerCase(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}