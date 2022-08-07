package tv.wlg.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

public class AppReply {
    private static final Properties configs = new Properties();
    private static final AppReply instance = new AppReply();
    private AppReply() {
        loadConfigurations();
    }
    private static void loadConfigurations() {
        File file = new File("./locale.properties");
        try(FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fileInputStream, Charset.forName("windows-1251"))) {
            configs.load(reader);
        } catch (Exception ignore) {}
    }

    public static String getRules() {
        return configs.getProperty("help.rules",
                "Помощь: " +
                        "1) ! - начало команды, " +
                        "2) Команды выполняются слева направо, " +
                        "3) !а - ввод, " +
                        "4) !a200ms - ввод с таймером(s/ms), " +
                        "5) _a - зажать кнопку, " +
                        "6) -a - отпустить кнопку, " +
                        "7) # - пустой ввод для задержки следущего ввода, " +
                        "8) a+b - нажимает A и B одновременно, " +
                        "9) [a+b]*2 - для повторений");
    }

    public static String getNESButtons() {
        return configs.getProperty("help.NES.buttons", "Controls NES: #(blank input), a, b, select, start, up, down, left, right");
    }

    public static String getSEGAButtons() {
        return configs.getProperty("help.SEGA.buttons", "Controls SEGA: #(blank input), a, b, c, x, y, z, select, start, up, down, left, right");
    }

    public static String getXBOXButtons() {
        return configs.getProperty("help.XBOX.buttons", "Controls XBOX: #(blank input), a, b, x, y, lb, rb, lt, rt, view, menu, ls, rs, up, down, left, right, lsup, lsdown, lsleft, lsright, rsup, rsdown, rsd, rsleft, rsright");
    }

    public static String getCommands() {
        return configs.getProperty("help.commands",
                "Available Commands: " +
                        "!nes, " +
                        "!sega, " +
                        "!xbox, " +
                        "!stop, " +
                        "!commands, " +
                        "!help, " +
                        "!kickvote, " +
                        "!adminlist, " +
                        "!adminremove, " +
                        "!adminremoveall, " +
                        "!whitelist, " +
                        "!wlremoveall, " +
                        "!wlremove, " +
                        "!blacklist" +
                        "!blremoveall" +
                        "!blremove");
    }

    public static String getMessageStop() {
        return configs.getProperty("message.stop", "Cancelling all the queues");
    }

    public static String getMessageRestart() {
        return configs.getProperty("message.restart", "Restarting the game");
    }

    public static String getMessageNewAdmin() {
        return configs.getProperty("message.adminlist", "Users now Admin:");
    }

    public static String getMessageRemovedAdmins() {
        return configs.getProperty("message.adminremove", "Users removed from Admin");
    }

    public static String getMessageRemoveAllAdmins() {
        return configs.getProperty("message.adminremoveall", "All users removed from Admin");
    }

    public static String getMessageWhiteList() {
        return configs.getProperty("message.whitelist", "Users whitelisted:");
    }

    public static String getMessageWLRemove() {
        return configs.getProperty("message.wlremove", "Users removed from WhiteList:");
    }

    public static String getMessageWLRemoveAll() {
        return configs.getProperty("message.wlremoveall", "All users removed from WhiteList");
    }

    public static String getMessageBlackList() {
        return configs.getProperty("message.blacklist", "Users blacklisted:");
    }

    public static String getMessageBLRemove() {
        return configs.getProperty("message.blremove", "Users removed from BlackList:");
    }

    public static String getMessageBLRemoveAll() {
        return configs.getProperty("message.blremoveall", "All users removed from BlackList");
    }

    public static String getMessageVoteKick(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.votekick", "User BlackListed(Votes):"), logArgs);
    }

    public static int getMessageVoteKickCount() {
        return Integer.parseInt(configs.getProperty("message.votekick.count", "10"));
    }

    public static String getMessageVoteKickStart(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.votekick.start", "Vote to add User to BlackList:"), logArgs);
    }

    public static String getMessageVoteKickVotes(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.votekick.votes", "Count of Votes required to kick"), logArgs);
    }

    public static String getMessageVoteKickTimeOut(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.votekick.timeout", "Vote TimeOut reached, not enough votes to kick:"), logArgs);
    }

    public static String getMessageVoteStop() {
        return configs.getProperty("message.votestop", "Cancelling all the queues(votes)");
    }

    public static String getMessageVoteStopStart() {
        return configs.getProperty("message.votestop.start", "Vote to Stop the execution:");
    }

    public static int getMessageVoteStopCount() {
        return Integer.parseInt(configs.getProperty("message.votestop.count", "5"));
    }

    public static String getMessageVoteStopVotes(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.votestop.votes", "Count of Votes required to stop:"), logArgs);
    }

    public static String getMessageVoteStopTimeOut() {
        return configs.getProperty("message.votestop.timeout", "Vote TimeOut reached, not enough votes to STOP the queues");
    }

    public static String getMessageVotePlay(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay", "@{EXCLUSIVE_USER}, Chat voted to give you exclusive access to control for {EXCLUSIVE_TIME} min)"), logArgs);
    }

    public static int getMessageVotePlayCount() {
        return Integer.parseInt(configs.getProperty("message.voteplay.count", "5"));
    }

    public static String getMessageVotePlayStart(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.start", "Vote, to give exclusive access to control for {EXCLUSIVE_TIME} min to: !voteplay{EXCLUSIVE_TIME} @{VOTE_FOR_USER}"), logArgs);
    }

    public static String getMessageVotePlayEnded(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.ended", "@{EXCLUSIVE_USER} your access to exclusive control been ended"), logArgs);
    }

    public static String getMessageVotePlayExist(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.exist", "@{EXCLUSIVE_USER} already has exclusive access(min remaining: {EXCLUSIVE_TIME})"), logArgs);
    }

    public static String getMessageVotePlayMAXTIME(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.maxtime", "@{USER}: Maximum available time for Exclusive access - 10 min: !voteplay10"), logArgs);
    }

    public static String getMessageVotePlayMINTIME(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.mintime", "@{USER}: Minimum available time for Exclusive access - 1 min: !voteplay1"), logArgs);
    }

    public static String getMessageVotePlayVotes(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.votes", "Votes required for exclusive access({EXCLUSIVE_TIME} min) for @{VOTE_FOR_USER} : {VOTES_SIZE}/5"), logArgs);
    }

    public static String getMessageVotePlayTimeOut(Map<String, String> logArgs) {
        return instance.replaceArguments(configs.getProperty("message.voteplay.timeout", "Time is over not enough votes: !voteplay{EXCLUSIVE_TIME} @{VOTE_FOR_USER}"), logArgs);
    }

    private String replaceArguments(String message, Map<String, String> args) {
        for (Map.Entry<String, String> entry : args.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return message;
    }
}
