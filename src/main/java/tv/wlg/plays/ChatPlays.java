package tv.wlg.plays;

import tv.wlg.chat.Message;
import tv.wlg.chat.TwitchChat;
import tv.wlg.concurrent.ThreadPoolManager;
import tv.wlg.config.AppConfig;
import tv.wlg.config.AppReply;
import tv.wlg.console.GamePad;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ChatPlays {
    private final TwitchChat twitchChat;
    private final ThreadPoolManager threadPoolManager = new ThreadPoolManager();

    private GamePad gamePad;

    private final Set<String> whiteList = readFromFile("./whitelist.txt");
    private final Set<String> blackList = readFromFile("./blacklist.txt");
    private final Set<String> adminList = readFromFile("./adminlist.txt");

    private volatile String exclusiveUser = "";
    private int exclusiveUserTime = 0;
    private ScheduledFuture<?> exclusiveTimeoutTask = null;
    private final HashMap<String, Set<String>> voteMap = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    ChatPlays() {
        //Connect to Twitch Chat
        TwitchChat twitchChat = new TwitchChat(this::onChatMessage, threadPoolManager);
        twitchChat.joinChannel(AppConfig.getChannel());

        this.twitchChat = twitchChat;
        this.gamePad = new GamePad(threadPoolManager, AppConfig.getDefaultGamePad());

        executorService.scheduleAtFixedRate(new TimerMessage("Управление доступно для всех желающих. Вы можете предоставить эксклюзивный доступ к управлению для определённого игрока через голосование: !voteplay10 @username"), 15, 15, TimeUnit.MINUTES);

        Path path = Paths.get("./action");
        try {
            Files.createDirectory(path);
        } catch (Exception ignore) {}
    }

    public static void main(String[] args) {
        System.out.println("Starting application");
        new ChatPlays();
    }

    private void onChatMessage(Message message) {
        Map<String, String> logArgs = new HashMap<>();
        if (!message.getCommand().equalsIgnoreCase("PRIVMSG")) {
            System.out.println(Thread.currentThread().getName() + " : " + (message.getUser() == null ? "SYSTEM" : message.getUser()) + "(" + message.getCommand() + ") : " + message.getMessage());
            return;
        }

        System.out.println(Thread.currentThread().getName() + " : " + message.getUser() + " : " + message.getMessage());
        if (!message.getMessage().startsWith("!")) {
            return;
        } else {
            message.setMessage(message.getMessage().substring(1));
        }

        if (message.getMessage().startsWith("adminlist")) {
            if (isSuperAdmin(message)) {
                addToList(adminList, "adminlist", "./adminlist.txt", AppReply.getMessageNewAdmin(), message);
            }

            printList(adminList);
            return;
        } else if (message.getMessage().startsWith("adminremoveall")) {
            if (isSuperAdmin(message)) {
                adminList.clear();
                twitchChat.sendMessage(AppReply.getMessageRemoveAllAdmins());

                deleteFile("./adminlist.txt");
            }

            return;
        } else if (message.getMessage().startsWith("adminremove")) {
            if (isSuperAdmin(message)) {
                removeFromList(adminList, "adminremove", "./adminlist.txt", AppReply.getMessageRemovedAdmins(), message);
            }

            return;
        } else if (message.getMessage().startsWith("whitelist")) {
            if (isAllowedToUse(message)) {
                addToList(whiteList, "whitelist", "./whitelist.txt", AppReply.getMessageWhiteList(), message);
            }

            printList(whiteList);
            return;
        } else if (message.getMessage().startsWith("wlremoveall")) {
            if (isAllowedToUse(message)) {
                whiteList.clear();
                twitchChat.sendMessage(AppReply.getMessageWLRemoveAll());

                deleteFile("./whitelist.txt");
            }

            return;
        } else if (message.getMessage().startsWith("wlremove")) {
            if (isAllowedToUse(message)) {
                removeFromList(whiteList, "wlremove", "./whitelist.txt", AppReply.getMessageWLRemove(), message);
            }

            return;
        } else if (message.getMessage().startsWith("blacklist")) {
            if (isAllowedToUse(message)) {
                addToList(blackList, "blacklist", "./blacklist.txt", AppReply.getMessageBlackList(), message);
            }

            printList(blackList);
            return;
        } else if (message.getMessage().startsWith("blremoveall")) {
            if (isAllowedToUse(message)) {
                blackList.clear();
                twitchChat.sendMessage(AppReply.getMessageBLRemoveAll());

                deleteFile("./blacklist.txt");
            }

            return;
        } else if (message.getMessage().startsWith("blremove")) {
            if (isAllowedToUse(message)) {
                removeFromList(blackList, "blremove", "./blacklist.txt", AppReply.getMessageBLRemove(), message);
            }

            return;
        } else if (message.getMessage().startsWith("votekick")) {
            if (isBlackListed(message)) {
                return;
            }

            String user = message.getMessage().substring("votekick".length()).trim().toLowerCase();
            if (user.startsWith("@")) {
                user = user.substring(1);
            }

            if (user.length() == 0) {
                return;
            }
            user = user.trim();

            logArgs.put("{VOTE_FOR_USER}", user);
            if (!voteMap.containsKey(user)) {
                twitchChat.sendMessage(AppReply.getMessageVoteKickStart(logArgs));
            }

            Set<String> votes = voteMap.get(user);
            if (votes == null) {
                votes = new HashSet<>();
                voteMap.put(user, votes);

                executorService.schedule(new VoteTimeout(user, AppReply.getMessageVoteKickTimeOut(logArgs)), 5, TimeUnit.MINUTES);
            }
            votes.add(message.getUser().toLowerCase());

            logArgs.put("{VOTES_SIZE}", "" + votes.size());
            if (votes.size() < AppReply.getMessageVoteKickCount()) {
                twitchChat.sendMessage(AppReply.getMessageVoteKickVotes(logArgs));
            } else {
                voteMap.remove(user);
                addToList(blackList,"./blacklist.txt", AppReply.getMessageVoteKick(logArgs), user);

                gamePad.cancel();
            }

            return;
        } else if (message.getMessage().startsWith("readmacro")) {
            return;
        } else if (message.getMessage().startsWith("newmacro")) {
            return;
        } else if (message.getMessage().startsWith("removemacro")) {
            return;
        } else if (message.getMessage().startsWith("voteplay")) {
            if (isBlackListed(message)) {
                return;
            }

            String[] args = message.getMessage().substring("voteplay".length()).trim().toLowerCase().split(" ");
            if (args.length < 2) {
                return;
            }

            if (exclusiveUser != null && !exclusiveUser.equalsIgnoreCase("")) {
                logArgs.put("{EXCLUSIVE_USER}", exclusiveUser);
                logArgs.put("{EXCLUSIVE_TIME}", "" + exclusiveUserTime);

                twitchChat.sendMessage(AppReply.getMessageVotePlayExist(logArgs));
                return;
            }

            int time;
            try {
                time = Integer.parseInt(args[0]);
            } catch (Exception ignore) {
                return;
            }

            String user = args[1];
            logArgs.put("{USER}", message.getUser());
            if (time > 10) {
                twitchChat.sendMessage(AppReply.getMessageVotePlayMAXTIME(logArgs));
                return;
            }
            if (time <= 0) {
                twitchChat.sendMessage(AppReply.getMessageVotePlayMINTIME(logArgs));
                return;
            }

            logArgs.put("{EXCLUSIVE_TIME}", "" + time);
            if (user.startsWith("@")) {
                user = user.substring(1);
            }

            user = user.trim();
            String key = user + ":" + time;
            logArgs.put("{VOTE_FOR_USER}", user);
            if (!voteMap.containsKey(key)) {
                twitchChat.sendMessage(AppReply.getMessageVotePlayStart(logArgs));
            }

            Set<String> votes = voteMap.get(key);
            if (votes == null) {
                votes = new HashSet<>();
                voteMap.put(key, votes);

                executorService.schedule(new VoteTimeout(key, AppReply.getMessageVotePlayTimeOut(logArgs)), 5, TimeUnit.MINUTES);
            }
            votes.add(message.getUser().toLowerCase());

            logArgs.put("{VOTES_SIZE}", "" + votes.size());
            if (votes.size() < AppReply.getMessageVotePlayCount()) {
                twitchChat.sendMessage(AppReply.getMessageVotePlayVotes(logArgs));
            } else {
                voteMap.remove(key);

                exclusiveUser = user;
                exclusiveUserTime = time;

                twitchChat.sendMessage(AppReply.getMessageVotePlay(logArgs));
                exclusiveTimeoutTask = executorService.scheduleAtFixedRate(new ExclusivePlayTimeout(), 1, 1, TimeUnit.MINUTES);
            }

            return;
        } else if (message.getMessage().startsWith("vpclear")) {
            if (isAllowedToUse(message)) {
                exclusiveUser = "";
                exclusiveUserTime = 0;

                if (exclusiveTimeoutTask != null) {
                    exclusiveTimeoutTask.cancel(true);
                }
            }
        }

        switch (message.getMessage()) {
            case "xbox":
                if (isAllowedToUse(message)) {
                    gamePad.cancel();
                    gamePad = new GamePad(threadPoolManager, "XBOX");
                    twitchChat.sendMessage("GamePad is now: XBOX");
                }

                break;
            case "sega":
                if (isAllowedToUse(message)) {
                    gamePad.cancel();
                    gamePad = new GamePad(threadPoolManager, "SEGA");
                    twitchChat.sendMessage("GamePad is now: SEGA");
                }

                break;
            case "nes":
                if (isAllowedToUse(message)) {
                    gamePad.cancel();
                    gamePad = new GamePad(threadPoolManager, "NES");
                    twitchChat.sendMessage("GamePad is now: NES");
                }

                break;
            case "votestop":
                String voteFor = "VoteStop_Commands_Queue";
                if (!voteMap.containsKey(voteFor)) {
                    twitchChat.sendMessage(AppReply.getMessageVoteStopStart());
                }

                Set<String> votes = voteMap.get(voteFor);
                if (votes == null) {
                    votes = new HashSet<>();
                    voteMap.put(voteFor, votes);

                    executorService.schedule(new VoteTimeout(voteFor, AppReply.getMessageVoteStopTimeOut()), 5, TimeUnit.MINUTES);
                }
                votes.add(message.getUser().toLowerCase());

                logArgs.put("{VOTES_SIZE}", "" + votes.size());
                if (votes.size() < AppReply.getMessageVoteStopCount()) {
                    twitchChat.sendMessage(AppReply.getMessageVoteStopVotes(logArgs));
                } else {
                    voteMap.remove(voteFor);
                    twitchChat.sendMessage(AppReply.getMessageVoteStop());
                    gamePad.cancel();
                }

                break;
            case "stop":
                if (isAllowedToUse(message)) {
                    gamePad.cancel();
                    twitchChat.sendMessage(AppReply.getMessageStop());
                }

                break;
            case "restart":
                if (isAllowedToUse(message)) {
                    gamePad.cancel();
                    gamePad.press("restart");
                    twitchChat.sendMessage(AppReply.getMessageRestart());
                }

                break;
            case "commands":
                twitchChat.sendMessage(AppReply.getCommands());

                break;
            case "help":
                twitchChat.sendMessage(gamePad.getRules());
                twitchChat.sendMessage(gamePad.getButtons());

                break;
            default:
                if (isInputAllowed(message)) {
                    gamePad.feed(message);
                }
        }
    }

    private void printList(Set<String> list) {
        String fullList = list.toString();
        if (fullList.length() > 450) {
            for (int start = 0; start < fullList.length(); start += 450) {
                int end = Math.min(start + 450, fullList.length());
                twitchChat.sendMessage(fullList.substring(start, end));
            }
        } else {
            twitchChat.sendMessage(fullList);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addToList(Set<String> list, String file, String replyMessage, String user) {
        list.add(user);
        saveToFile(list, file);
        twitchChat.sendMessage(replyMessage + " " + user);
    }
    private void addToList(Set<String> list, String command, String file, String replyMessage, Message message) {
        String users = message.getMessage().substring(command.length()).trim().toLowerCase();
        if (!users.equalsIgnoreCase("")) {
            String[] usersList = users.split(",");
            for (String user : usersList) {
                if (user.startsWith("@")) {
                    user = user.substring(1);
                }
                list.add(user.trim());
            }

            saveToFile(list, file);
            if (usersList.length > 0) {
                twitchChat.sendMessage(replyMessage + " " + users);
            }
        }
    }

    private void removeFromList(Set<String> list, String command, String file, String replyMessage, Message message) {
        String users = message.getMessage().substring(command.length()).trim().toLowerCase();
        String[] usersList = users.split(",");
        for (String user : usersList) {
            if (user.startsWith("@")) {
                user = user.substring(1);
            }
            list.remove(user.trim());
        }

        if (usersList.length > 0) {
            twitchChat.sendMessage(replyMessage + " " + users);
        }

        if (list.isEmpty()) {
            deleteFile(file);
        } else {
            saveToFile(list, file);
        }
    }

    private boolean isSuperAdmin(Message message) {
        if (message.getBadges() != null && message.getBadges().contains("broadcaster/1")) {
            System.out.println("STREAM ENTRY");
            return true;
        }

        if (message.isModer()) {
            System.out.println("MODER ENTRY");
            return true;
        }

        return false;
    }

    private boolean isAllowedToUse(Message message) {
        if (!adminList.isEmpty() && adminList.contains(message.getUser().toLowerCase())) {
            System.out.println("ADMIN ENTRY");
            return true;
        }

        return isSuperAdmin(message);
    }

    private boolean isBlackListed(Message message) {
        return !blackList.isEmpty() && blackList.contains(message.getUser().toLowerCase());
    }

    private boolean isInputAllowed(Message message) {
        if (isAllowedToUse(message)) {
            return true;
        }

        if (isBlackListed(message)) {
            System.out.println("USER BLACKLISTED");
            return false;
        }

        if (!exclusiveUser.equalsIgnoreCase("")) {
            if (exclusiveUser.equalsIgnoreCase(message.getUser())) {
                return true;
            }

            twitchChat.sendMessage("@" + message.getUser() + " У игрока @" + exclusiveUser + " эксклюзивный доступ к управлению(минут осталось " + exclusiveUserTime + ")");
            return false;
        }

        if (!whiteList.isEmpty() && whiteList.contains(message.getUser().toLowerCase())) {
            return true;
        }

        //noinspection RedundantIfStatement
        if (whiteList.size() > 0) {
            return false;
        }

        return true;
    }

    private void saveToFile(Set<String> set, String filePath) {
        deleteFile(filePath);
        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            for (String user : set) {
                printWriter.println(user);
            }
            printWriter.flush();
        } catch (Exception ignore) {}
    }

    private void deleteFile(String filePath) {
        try {
            Files.delete(Path.of(filePath));
        } catch (IOException ignore) {}
    }

    private Set<String> readFromFile(String filePath) {
        Set<String> set = new HashSet<>();

        try (FileReader fileReader = new FileReader(filePath);
                BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                set.add(line);
                line = bufferedReader.readLine();
            }
        } catch (Exception ignore) {}

        return set;
    }

    private class VoteTimeout implements Runnable {
        String voteFor;
        String message;

        VoteTimeout(String voteFor, String message) {
            this.voteFor = voteFor;
            this.message = message;
        }

        @Override
        public void run() {
            if (voteMap.containsKey(voteFor)) {
                voteMap.remove(voteFor);
                twitchChat.sendMessage(message);
            }
        }
    }

    private class ExclusivePlayTimeout implements Runnable {
        @Override
        public void run() {
            exclusiveUserTime = exclusiveUserTime - 1;
            if (exclusiveUserTime <= 0) {
                Map<String, String> logArgs = new HashMap<>() {{
                    put("{EXCLUSIVE_USER}", exclusiveUser);
                }};
                twitchChat.sendMessage(AppReply.getMessageVotePlayEnded(logArgs));

                exclusiveUser = "";
                exclusiveTimeoutTask.cancel(false);
            }
        }
    }

    private class TimerMessage implements Runnable {
        String message;

        TimerMessage(String messagee) {
            this.message = messagee;
        }

        public void run() {
            if ((exclusiveUser == null || exclusiveUser.equalsIgnoreCase("")) && whiteList.isEmpty()) {
                twitchChat.sendMessage(message);
            }
        }
    }
}