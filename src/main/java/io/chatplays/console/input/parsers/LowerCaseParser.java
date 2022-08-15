package io.chatplays.console.input.parsers;

import io.chatplays.console.input.Parser;

public class LowerCaseParser implements Parser {
    @Override
    public String parse(String commands) {
        return commands.toLowerCase();
    }
}
