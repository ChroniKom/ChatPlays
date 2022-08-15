package tv.wlg.console;

import io.chatplays.console.input.Parser;
import io.chatplays.console.input.Validator;
import io.chatplays.console.input.parsers.*;
import io.chatplays.console.input.validator.*;
import redlaboratory.jvjoyinterface.VJoy;
import redlaboratory.jvjoyinterface.VjdStat;
import tv.wlg.chat.Message;
import tv.wlg.concurrent.ThreadPoolManager;
import tv.wlg.config.AppReply;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GamePad {
    private final String helpButtons;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, Integer> buttons;
    private final Pattern pattern;

    private final VJoy vJoy;
    private final int deviceId = 1;

    private final ThreadPoolManager threadPoolManager;

    private final Queue<Future<?>> sequenceQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ScheduledFuture<?>> sequence = new ConcurrentLinkedQueue<>();

    private static final Path userCommand = Paths.get("./action/command.txt");

    private final List<Parser> parsers;
    private final List<Validator> validators;

    public GamePad(ThreadPoolManager threadPoolManager, String type) {
        String regex;
        if (type.equalsIgnoreCase("xbox")) {
            System.out.println("GAMEPAD: Включен XBOX GamePad");
            regex = "(?<hold>_)?(?<release>-)?(?<input>" +
                    "#|a|b|x|y|" +
                    "lb|rb|lt|rt|" +
                    "view|menu|" +
                    "lsup|lsu|lsdown|lsd|lsleft|lsl|lsright|lsr|" +
                    "rsup|rsu|rsdown|rsd|rsleft|rsl|rsright|rsr|" +
                    "ls|rs|" +
                    "up|u|down|d|left|l|right|r" +
                    ")(?<time>(?<duration>\\d+)(?<type>s|ms))?(?<plus>\\+)?";
            this.helpButtons = AppReply.getXBOXButtons();
            this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
            this.buttons = new HashMap<>() {{
                put("#", 0);
                put("a", 1);
                put("b", 2);
                put("x", 3);
                put("y", 4);

                put("lb", 5);
                put("rb", 6);
                put("lt", 7);
                put("rt", 8);

                put("view", 9);
                put("menu", 10);

                put("ls", 11);
                put("rs", 12);

                put("up", 13);
                put("u", 13);
                put("down", 14);
                put("d", 14);
                put("left", 15);
                put("l", 15);
                put("right", 16);
                put("r", 16);

                put("lsup", 19);
                put("lsu", 19);
                put("lsdown", 20);
                put("lsd", 20);
                put("lsleft", 21);
                put("lsl", 21);
                put("lsright", 22);
                put("lsr", 22);

                put("rsup", 23);
                put("rsu", 23);
                put("rsdown", 24);
                put("rsd", 24);
                put("rsleft", 25);
                put("rsl", 25);
                put("rsright", 26);
                put("rsr", 26);

                put("restart", 99);
            }};
        } else if (type.equalsIgnoreCase("sega")) {
            System.out.println("GAMEPAD: Включен SEGA GamePad");
            regex = "(?<hold>_)?(?<release>-)?(?<input>" +
                    "#|a|b|c|x|y|z|" +
                    "select|start|" +
                    "up|u|down|d|left|l|right|r" +
                    ")(?<time>(?<duration>\\d+)(?<type>s|ms))?(?<plus>\\+)?";
            this.helpButtons = AppReply.getSEGAButtons();
            this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
            this.buttons = new HashMap<>() {{
                put("#", 0);
                put("a", 1);
                put("b", 2);
                put("x", 3);
                put("y", 4);

                put("select", 9);
                put("start", 10);

                put("up", 13);
                put("u", 13);
                put("down", 14);
                put("d", 14);
                put("left", 15);
                put("l", 15);
                put("right", 16);
                put("r", 16);

                put("c", 19);
                put("z", 20);

                put("restart", 99);
            }};
        } else {
            System.out.println("GAMEPAD: Включен NES GamePad");
            regex = "(?<hold>_)?(?<release>-)?(?<input>" +
                    "#|a|b|" +
                    "select|start|" +
                    "up|u|down|d|left|l|right|r" +
                    ")(?<time>(?<duration>\\d+)(?<type>s|ms))?(?<plus>\\+)?";
            this.helpButtons = AppReply.getNESButtons();
            this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
            this.buttons = new HashMap<>() {{
                put("#", 0);
                put("a", 1);
                put("b", 2);

                put("select", 9);
                put("start", 10);

                put("up", 13);
                put("u", 13);
                put("down", 14);
                put("d", 14);
                put("left", 15);
                put("l", 15);
                put("right", 16);
                put("r", 16);

                put("restart", 99);
            }};
        }

        this.parsers = new ArrayList<>();
        this.parsers.add(new TrimParser());
        this.parsers.add(new LowerCaseParser());
        this.parsers.add(new RepeatParser());
        this.parsers.add(new ShortTimeParser(pattern));

        this.validators = new ArrayList<>();
        this.validators.add(new CommandValid(pattern));

        //Initialize VJoy gamepad
        this.vJoy = initVJoy();
        this.threadPoolManager = threadPoolManager;
    }

    public void feed(Message message) {
        String originalMessage = message.getMessage();

        for (Parser parser : parsers) {
            message.setMessage(parser.parse(message.getMessage()));
        }

        for (Validator validator : validators) {
            if (!validator.isValid(message.getMessage())) {
                return;
            }
        }

        Future<?> future = threadPoolManager.getSequenceExecutor().submit(new ParserExecutor(message, originalMessage));
        sequenceQueue.add(future);
    }

    public void cancel() {
        while (!this.sequenceQueue.isEmpty()) {
            Future<?> future = this.sequenceQueue.poll();
            if (future != null) {
                future.cancel(true);
            }
        }
        while (!this.sequence.isEmpty()) {
            ScheduledFuture<?> scheduledFuture = this.sequence.poll();
            if (scheduledFuture != null && !scheduledFuture.isDone()) {
                scheduledFuture.cancel(true);
            }
        }

        vJoy.resetAll();
        vJoy.resetVJD(deviceId);
        vJoy.resetPovs(deviceId);
        vJoy.resetButtons(deviceId);
    }

    public String getRules() {
        return AppReply.getRules();
    }

    public String getButtons() {
        return helpButtons;
    }

//    public String getControls() {
//        return "Controls: " +
//                "(2): !a - press A button " +
//                "(3): !a = !a200ms - press A for 200ms(default, can be ms/s) " +
//                "(4): !a b up - press A then B then UP " +
//                "(5): !_right a - press and hold RIGHT then press A(RIGHT still holds) " +
//                "(6): !_right a -right - hold RIGHT, press A, release RIGHT " +
//                "(7): !_right a # -right - hold RIGHT, press A, wait 200ms, release RIGHT " +
//                "(8): !a+b - press A and B together " +
//                "(9): !_right [a+b]*2 -right - hold right, press A and B together(repeat 2 times), release RIGHT";
//    }

    public void press(String button) {
        vJoy.setBtn(true, deviceId, buttons.get(button));
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignore) {}
        vJoy.setBtn(false, deviceId, buttons.get(button));
    }

    private VJoy initVJoy() {
        VJoy vJoy = new VJoy();

        if (!vJoy.vJoyEnabled()) {
            System.out.println("vJOY: driver not enabled - Failed Getting vJoy attributes");

            throw new RuntimeException("vJOY: Enable vJoy before start");
        } else {
            System.out.println("vJOY: Vendor: " + vJoy.getvJoyManufacturerString());
            System.out.println("vJOY: Product: " + vJoy.getvJoyProductString());
            System.out.println("vJOY: Version Number: " + vJoy.getvJoyVersion());
        }

        if (vJoy.driverMatch()) {
            System.out.println("vJOY: Version of Driver Matches DLL Version {0}");
        } else {
            System.out.println("vJOY: Version of Driver {0} does NOT match DLL Version {1}");
        }

        VjdStat status = vJoy.getVJDStatus(deviceId);
        if ((status == VjdStat.VJD_STAT_OWN) ||
                ((status == VjdStat.VJD_STAT_FREE) && (!vJoy.acquireVJD(deviceId)))) {
            System.out.println("vJOY: Failed to acquire vJoy device number " + deviceId);
        } else {
            System.out.println("vJOY: Acquired - vJoy device number " + deviceId);
        }

        System.out.println("vJOY: number of buttons - " + vJoy.getVJDButtonNumber(deviceId));

        return vJoy;
    }

    private class ParserExecutor implements Runnable {
        @SuppressWarnings("FieldCanBeLocal")
        private final long defaultDelay = 200;
        private final Message message;
        private final String originalMessage;

        ParserExecutor(Message message, String originalMessage) {
            this.message = message;
            this.originalMessage = originalMessage;
        }

        @Override
        public void run() {
            final Matcher matcher = pattern.matcher(message.getMessage());

            try {
                Files.write(userCommand, (message.getUser() + ": " + originalMessage).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            vJoy.resetAll();
            vJoy.resetVJD(deviceId);
            vJoy.resetPovs(deviceId);
            vJoy.resetButtons(deviceId);

            sequence.clear();
            long await = 0;
            long customAwait = 0;
            long holdReleaseAwait = 0;
            boolean isHoldRelease = false;
            while (matcher.find()) {
                ScheduledFuture<?> scheduledFuture;
                if (matcher.group("hold") != null && !matcher.group("hold").isEmpty()) {
                    isHoldRelease = true;

                    scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(true, deviceId, buttons.get(matcher.group("input"))), await, TimeUnit.MILLISECONDS);
                    sequence.add(scheduledFuture);

                    if (matcher.group("duration") != null && !matcher.group("duration").isEmpty()) {
                        long duration = Long.parseLong(matcher.group("duration"));
                        if (matcher.group("type") != null && matcher.group("type").equalsIgnoreCase("s")) {
                            duration = duration * 1000;
                        }
                        if (holdReleaseAwait < duration) {
                            holdReleaseAwait = duration;
                        }

                        scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(false, deviceId, buttons.get(matcher.group("input"))), await + duration, TimeUnit.MILLISECONDS);
                        sequence.add(scheduledFuture);
                    }
                } else if (matcher.group("release") != null && !matcher.group("release").isEmpty()) {
                    isHoldRelease = true;

                    scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(false, deviceId, buttons.get(matcher.group("input"))), await, TimeUnit.MILLISECONDS);
                    sequence.add(scheduledFuture);

                    if (matcher.group("duration") != null && !matcher.group("duration").isEmpty()) {
                        long duration = Long.parseLong(matcher.group("duration"));
                        if (matcher.group("type") != null && matcher.group("type").equalsIgnoreCase("s")) {
                            duration = duration * 1000;
                        }
                        if (holdReleaseAwait < duration) {
                            holdReleaseAwait = duration;
                        }

                        scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(true, deviceId, buttons.get(matcher.group("input"))), await + duration, TimeUnit.MILLISECONDS);
                        sequence.add(scheduledFuture);
                    }
                } else {
                    scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(true, deviceId, buttons.get(matcher.group("input"))), await, TimeUnit.MILLISECONDS);
                    sequence.add(scheduledFuture);
                    if (matcher.group("duration") != null && !matcher.group("duration").isEmpty()) {
                        long duration = Long.parseLong(matcher.group("duration"));
                        if (matcher.group("type") != null && matcher.group("type").equalsIgnoreCase("s")) {
                            duration = duration * 1000;
                        }

                        if (customAwait < duration) {
                            customAwait = duration;
                        }

                        scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(false, deviceId, buttons.get(matcher.group("input"))), await + duration, TimeUnit.MILLISECONDS);
                        sequence.add(scheduledFuture);
                    } else {
                        scheduledFuture = threadPoolManager.getSequenceWorker().schedule(new ButtonState(false, deviceId, buttons.get(matcher.group("input"))), await + defaultDelay, TimeUnit.MILLISECONDS);
                        sequence.add(scheduledFuture);
                    }
                }

                if (matcher.group("plus") != null && !matcher.group("plus").isEmpty()) {
                    continue;
                }

                if (isHoldRelease) {
                    isHoldRelease = false;
                    continue;
                }

                //await - time between sequence commands - default 200
                //customAwait - in case user specified custom time for button
                //defaultDelat - default "customAwait" time for button = 200ms
                //50 - time between buttons: a ${time} a ${time} a
                if (customAwait != 0) {
                    await = await + customAwait + 50;
                    customAwait = 0;
                } else {
                    await = await + defaultDelay + 50;
                }
            }

            try {
                Thread.sleep(Math.max(await, holdReleaseAwait) + 200);
            } catch (InterruptedException ignore) {
                return;
            } finally {
                vJoy.resetAll();
                vJoy.resetVJD(deviceId);
                vJoy.resetPovs(deviceId);
                vJoy.resetButtons(deviceId);
            }

            try {
                sequenceQueue.remove();
            } catch (Exception ignore) {}
        }
    }

    private class ButtonState implements Runnable {
        boolean state;
        int deviceId;
        int button;

        ButtonState(boolean state, int deviceId, int button) {
            this.state = state;
            this.deviceId = deviceId;
            this.button = button;
        }

        @Override
        public void run() {
            vJoy.setBtn(state, deviceId, button);
        }
    }
}
