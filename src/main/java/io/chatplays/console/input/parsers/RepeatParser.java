package io.chatplays.console.input.parsers;

import io.chatplays.console.input.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatParser implements Parser {
    @SuppressWarnings("RegExpRedundantEscape")
    private static final String expandRegex = "\\[([^\\[\\]]*)\\](\\*|x)(\\d{1,2})";
    private final Pattern expandPattern = Pattern.compile(expandRegex, Pattern.MULTILINE);

    @Override
    public String parse(String command) {
        final Matcher matcher = expandPattern.matcher(command);

        StringBuilder stringBuilder = new StringBuilder();
        int lastMatch = 0;
        while (matcher.find()) {
            stringBuilder.append(command, lastMatch, matcher.start());
            if (matcher.group(3) != null) {
                int repeat = Integer.parseInt(matcher.group(3));
                stringBuilder.append(String.valueOf(matcher.group(1)).repeat(Math.max(0, repeat)));
            }
            lastMatch = matcher.end();
        }
        stringBuilder.append(command, lastMatch, command.length());

        return stringBuilder.toString();
    }
}
