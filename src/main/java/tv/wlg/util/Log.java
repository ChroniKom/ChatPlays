package tv.wlg.util;

import java.util.Map;

public class Log {
    public static void print(String message, Map<String, String> args) {
        for (Map.Entry<String, String> entry : args.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        System.out.println(message);
    }
}
