package com.koushik.springaicode.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
    public String extract(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
