package io.chatplays.console.input.parsers;

import io.chatplays.console.input.Parser;

public class TrimParser implements Parser {
    @Override
    public String parse(String command) {
        return command.trim().replace(" ", "");
    }
}
