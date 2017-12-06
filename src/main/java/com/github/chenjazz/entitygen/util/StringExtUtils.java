package com.github.chenjazz.entitygen.util;


/**
 * @author 陈佳志
 * 2017-09-03
 */
public class StringExtUtils {


    /**
     * 驼峰转下划线  camelToUnderline -> camel_to_underline
     *
     * @param param 驼峰形式的字符串
     * @return 下划线形式的字符串
     */
    public static String camelToUnderline(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        param = param.toLowerCase();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_");
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        String temp = sb.toString();
        if (temp.startsWith("_")) {
            return temp.substring(1);
        }
        return temp;

    }

    /**
     * 下划线转驼峰  underline_to_camel -> underlineToCamel
     *
     * @param param 下划线形式的字符串
     * @return 驼峰形式的字符串
     */
    public static String underlineToCamel(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        param = param.toLowerCase();
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == '_') {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static boolean isFirstLowerCamel(String str) {
        if (str.contains("_")) {
            return false; //不含有_
        }
        char[] chars = str.toCharArray();
        if (chars[0] >= 65 && chars[0] <= 90) {
            return false;//第一个字母不能是大写
        }
        for (int i = 1; i < chars.length; i++) {
            //必须含有大写
            if (chars[i] >= 65 && chars[i] <= 90) {
                return true;
            }
        }

        return false;
    }

//    public static void main(String[] args) {
//        System.out.println(isFirstLowerCamel("hiName"));
//        System.out.println(isFirstLowerCamel("his"));
//        System.out.println(isFirstLowerCamel("ABC"));
//        System.out.println(isFirstLowerCamel("aBcD"));
//        System.out.println(isFirstLowerCamel("aB_cD"));
//    }


    public static String firstCharUpper(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String firstCharLower(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static String pluralToSingular(String name) {
        //TODO
        if (!name.endsWith("s")) {
            return name;
        } else if (name.endsWith("ss")) {
            return name;
        } else {
            return name.substring(0, name.length() - 1);
        }
    }
}
