package com.yube.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {

    public static List<String> getJsonListByContainingField(String text, String field) {
        Pattern tagPattern = Pattern.compile(String.format("\"%s\":\\{.+\\}", field));
        Matcher tagMatcher = tagPattern.matcher(text);
        List<String> result = new ArrayList<>();
        while (tagMatcher.find()) {
            int startIndex = tagMatcher.start();
            int i = startIndex;
            for (; i >= 0; i--) {
                if (text.charAt(i) == '{') break;
            }
            startIndex = i;
            int endIndex = findJsonEndIndexByStartIndex(startIndex, text);
            result.add(text.substring(startIndex, endIndex));
        }
        return result;
    }

    private static int findJsonEndIndexByStartIndex(int startIndex, String text) {
        int bracesDiffCount = 0;
        if (text.length() < 2 || text.charAt(startIndex) != '{') {
            throw new IllegalArgumentException("Passed string is not valid json");
        }
        for (int i = startIndex; i < text.length(); i++) {
            if (text.charAt(i) == '{') bracesDiffCount++;
            if (text.charAt(i) == '}') bracesDiffCount--;
            if (bracesDiffCount == 0) return i + 1;
        }
        throw new IllegalArgumentException("Passed string is not valid json, opening and closing braces count is different");
    }

    public static List<JSONArray> getJsonArrays(JSONArray array) {
        List<JSONArray> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONArray(i) != null) {
                result.add(array.getJSONArray(i));
            }
        }
        return result;
    }

    public static List<JSONObject> getJsonObjects(JSONArray array) {
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONObject(i) != null) {
                result.add(array.getJSONObject(i));
            }
        }
        return result;
    }

    public static String getJsonObjectTagValue(JSONObject obj, String tagName, boolean acceptNulls) {
        if (!obj.has(tagName))
            throw new IllegalArgumentException("Json Object doesn't have such tag");
        if (obj.opt(tagName) == null) {
            if (acceptNulls) return null;
            throw new IllegalArgumentException("Json Object tag value is null");
        }
        if (!(obj.opt(tagName) instanceof String))
            throw new IllegalArgumentException("Tag with such name has value that different from string");
        return (String) obj.get(tagName);
    }

    public static String getJsonObjectTagValue(JSONObject obj, String tagName, String replacement, boolean acceptNulls) {
        if (!obj.has(tagName)) return replacement;
        if (obj.opt(tagName) == null) {
            if (acceptNulls) return replacement;
            throw new IllegalArgumentException("Json Object tag value is null");
        }
        if (!(obj.opt(tagName) instanceof String))
            throw new IllegalArgumentException("Tag with such name has value that different from string");
        return obj.getString(tagName);
    }
}
