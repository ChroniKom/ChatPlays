package tv.wlg.chat;

import jakarta.websocket.*;
import tv.wlg.concurrent.ThreadPoolManager;
import tv.wlg.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@ClientEndpoint
public class TwitchChat {
    private final ScheduledExecutorService service;
    private ScheduledFuture<?> restarter;
    private final Consumer consumer;
    private Session session;

    public TwitchChat(Consumer consumer, ThreadPoolManager threadPoolManager) {
        this.consumer = consumer;
        this.service = threadPoolManager.getChatMessageProcessor();

        connect();
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("CHAT: session is Opened");

        this.session = session;
        openSession();

        service.submit(() -> {});
        if (restarter != null) {
            restarter.cancel(false);
            restarter = null;
        }
    }

    @OnMessage
    public void onMessage(String chatMessage) {
        service.submit(new ParseMessage(chatMessage));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("CHAT: some ERROR is happened: " + throwable);
        throwable.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("CHAT: session is closed: " + reason);

        this.restarter = service.scheduleAtFixedRate(this::connect, 0, 30, TimeUnit.SECONDS);
    }

    public void joinChannel(String channel) {
        try {
            this.session.getBasicRemote().sendText(String.format("JOIN #%s", channel.toLowerCase()));
        } catch (IOException e) {
            System.out.println("CHAT: cannot join chanel: " + e);
        }
    }

    public void leaveChannel(String channel) {
        try {
            this.session.getBasicRemote().sendText(String.format("PART #%s", channel.toLowerCase()));
        } catch (IOException e) {
            System.out.println("CHAT: cannot leave channel: " + e);
        }
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(String.format("PRIVMSG #%s :%s\r\n", AppConfig.getChannel(), "/me " + message));
        } catch (Exception e) {
            System.out.println("CHAT: cannot send message: " + e);
        }
    }

    private void connect() {
        System.out.println("CHAT: trying to Connect to Twitch Chat");
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(AppConfig.getIRC()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to Chat: " + e, e);
        }
    }

    private void openSession() {
        try {
            session.getBasicRemote().sendText(String.format("PASS %s", AppConfig.getToken()));
            session.getBasicRemote().sendText(String.format("NICK %s", AppConfig.getUser()));
            session.getBasicRemote().sendText(String.format("USER TwitchIRC(%s) v1.0", AppConfig.getUser()));
            session.getBasicRemote().sendText("CAP REQ :twitch.tv/tags");
            session.getBasicRemote().sendText("CAP REQ :twitch.tv/commands");
            session.getBasicRemote().sendText("CAP REQ :twitch.tv/membership");
            session.getBasicRemote().flushBatch();
        } catch (Exception e) {
            System.out.println("CHAT: cannot open session: " + e);
        }
    }

    private void pongReply() {
        try {
            this.session.getBasicRemote().sendText("PONG :tmi.twitch.tv");
        } catch (Exception e) {
            System.out.println("CHAT: ping/pong error: " + e);
        }
    }

    @SuppressWarnings("UnusedAssignment")
    private class ParseMessage implements Runnable {
        String chatMessage;

        ParseMessage(String chatMessage) {
            this.chatMessage = chatMessage;
        }

        @Override
        public void run() {
            if (this.chatMessage.equalsIgnoreCase("PING :tmi.twitch.tv\r\n")) {
                pongReply();
            }

            Message message = new Message();
            int index = 0;

            // The raw components of the IRC message
            String rawTagsComponent = null;
            String rawUserComponent = null;
            String rawCommandComponent = null;
            String rawParametersComponent = null;

            // If the message includes tags, get the tags component of the IRC message
            if (this.chatMessage.charAt(index) == '@') { // The message includes tags
                int endIndex = chatMessage.indexOf(' ');
                rawTagsComponent = chatMessage.substring(1, endIndex);
                index = endIndex + 1; // Should now point to source colon (:)
            }

            // Get the source component (nick and host) of the IRC message.
            // The index should point to the source(User) part;
            if (this.chatMessage.charAt(index) == ':') {
                index += 1;
                int endIndex = chatMessage.indexOf(' ', index);
                rawUserComponent = chatMessage.substring(index, endIndex);
                index = endIndex + 1; // Should point to the User Message part of the IRC chatMessage.
            }


            // Get the command component of the IRC message.
            {
                int endIndex = chatMessage.indexOf(':', index);
                if (endIndex == -1) {
                    endIndex = chatMessage.length();
                }
                rawCommandComponent = chatMessage.substring(index, endIndex).trim();

                // Get the parameters component of the IRC message
                if (endIndex != chatMessage.length()) { // Check if the IRC message contains a parameters component
                    index = endIndex + 1;               // Should point to the parameters part of the message
                    rawParametersComponent = chatMessage.substring(index);
                }
            }

            // Parse the command component of the IRC message.
            parseCommand(rawCommandComponent, message);

            // Parse the rest of the components if it's a command we care about
            if (message.getCommand() != null) {
                if (rawTagsComponent != null) {
                    parseTags(rawTagsComponent, message);
                }
                message = parseUser(rawUserComponent, message);
                if (rawParametersComponent != null) {
                    message.setMessage(rawParametersComponent.replace("\r\n", ""));
                } else {
                    message.setMessage(null);
                }
            } else { // null if it's a message we don't care about.
                return;
            }

            consumer.process(message);
        }

        private void parseCommand(String rawCommandComponent, Message message) {
            String[] components = rawCommandComponent.split(" ");
            switch (components[0]) {
                case "JOIN":
                case "PART":
                case "NOTICE":
                case "CLEARCHAT":
                case "HOSTTARGET":
                case "PRIVMSG":
                case "USERSTATE": // Included only if you request the /commands capability.
                case "ROOMSTATE": // Included only if you request the /commands capability.
                    message.setCommand(components[0]);
                    message.setChannel(components[1]);

                    break;

                case "PING":
                case "GLOBALUSERSTATE": // Included only if you request the /commands capability.
                    message.setCommand(components[0]);

                    break;

                case "CAP":
                    message.setCommand(components[0]);
                    if (components[2].equals("ACK")) {
                        System.out.println("CHAT: CAP Request: Enabled");
                    } else {
                        System.out.println("CHAT: CAP Request: Disabled");
                    }

                    break;

                case "RECONNECT":
                    System.out.println("CHAT: TwitchIRC is about to Terminate the Session for Maintenance");
                    message.setCommand(components[0]);

                    break;

                case "421":
                    System.out.println("CHAT: unsupported IRC Command: " + components[0]);
                    message.setCommand(components[0]);

                    break;

                case "001":
                    System.out.println("CHAT: logged in: SUCCESSFULLY");
                    message.setCommand(components[0]);
                    message.setChannel(components[1]);

                    break;

                case "002": // Ignoring all other numeric messages.
                case "003":
                case "004":
                case "353": // Tells you who else is in the chat room you're joining.
                case "366":
                case "372":
                case "375":
                case "376":
                    System.out.println("CHAT: numeric message: " + components[0]);
                    message.setCommand(components[0]);

                    break;

                default:
                    System.out.println("CHAT: unrecognized IRC command: " + components[0]);
                    message.setCommand(components[0]);
            }
        }

        // Parses the tags component of the IRC message
        private void parseTags(String rawTagsComponent, Message message) {
            // badge-info=;badges=broadcaster/1;color=#0000FF;...

            String[] components = rawTagsComponent.split(";");
            for (String component : components) {
                String[] tag = component.split("=");
                String key = tag[0];
                String value = tag.length == 2 ? tag[1] : null;
                switch (key) { //Tag Key
                    case "client-nonce": //ignore tags
                    case "flags": //ignore tags
                    case "emotes":
                    case "emotes-sets":
                        break;

                    case "badges":
                    case "badges-info":
                        if (value != null) {
                            String[] badges = value.split(",");
                            for (String badge : badges) {
                                message.addBadge(badge);
                            }
                        }
                        break;

                    case "mod":
                        if (value != null && value.equalsIgnoreCase("1")) {
                            message.setModer(true);
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        private Message parseUser(String rawUserComponent, Message message) {
            if (rawUserComponent == null) {
                return message;
            }

            String[] components = rawUserComponent.split("!");
            if (components.length == 2) {
                message.setUser(components[0]);
                message.setHost(components[1]);
            } else {
                message.setUser(null);
                message.setHost(components[0]);
            }

            return message;
        }
    }
}
