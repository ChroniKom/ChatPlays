package io.chatplays.console.input.parsers;

import io.chatplays.console.input.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortTimeParser implements Parser {
    @SuppressWarnings("RegExpRedundantEscape")
    private static final String expandTimeRegex = "\\[([^\\[\\]]*)\\](?<time>(?<duration>\\d+)(?<type>ms|s))";
    private final Pattern expandTimePattern = Pattern.compile(expandTimeRegex, Pattern.MULTILINE);
    private final Pattern pattern;

    public ShortTimeParser(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public String parse(String command) {
        final Matcher matcher = expandTimePattern.matcher(command);

        StringBuilder stringBuilder = new StringBuilder();
        int lastMatch = 0;
        while (matcher.find()) {
            stringBuilder.append(command, lastMatch, matcher.start());
            if (matcher.group("time") != null) {
                String time = matcher.group("time");

                final Matcher inputMatcher = pattern.matcher(matcher.group(1));
                StringBuilder inputString = new StringBuilder();
                while (inputMatcher.find()) {
                    if (inputMatcher.group("time") == null) {
                        if (inputMatcher.group("plus") == null) {
                            inputString.append(inputMatcher.group(0)).append(time);
                        } else {
                            inputString.append(inputMatcher.group(0), 0, inputMatcher.group(0).length() - 1).append(time).append("+");
                        }
                    } else {
                        inputString.append(inputMatcher.group(0));
                    }
                }
                stringBuilder.append(inputString);
            }
            lastMatch = matcher.end();
        }
        stringBuilder.append(command, lastMatch, command.length());

        return stringBuilder.toString();
    }
}
