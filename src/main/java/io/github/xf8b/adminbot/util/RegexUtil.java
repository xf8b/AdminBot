package io.github.xf8b.adminbot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
    public static boolean containsIllegals(String toExamine) {
        Pattern pattern = Pattern.compile("[*+{}<>\\[\\]|\"_^$]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }
}
