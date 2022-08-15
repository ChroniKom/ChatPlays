package io.chatplays.console.input.validator;

import io.chatplays.console.input.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandValid implements Validator {
    private final Pattern pattern;

    public CommandValid(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean isValid(String command) {
        final Matcher matcher = pattern.matcher(command);

        int lastMatch = 0;
        while (matcher.find()) {
            if (lastMatch != matcher.start()) {
                return false;
            }

            lastMatch = matcher.end();
        }

        return lastMatch == command.length();
    }
}
