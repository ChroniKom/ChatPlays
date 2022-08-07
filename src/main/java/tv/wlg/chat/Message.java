package tv.wlg.chat;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private List<String> badges;
    private boolean isModer;
    private String user;
    private String host;
    private String command;
    private String channel;
    private String message;

    public List<String> getBadges() {
        return badges;
    }

    public void addBadge(String badge) {
        if (badges == null) {
            badges = new ArrayList<>();
        }

        this.badges.add(badge);
    }

    public boolean isModer() {
        return isModer;
    }

    public void setModer(boolean moder) {
        isModer = moder;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
