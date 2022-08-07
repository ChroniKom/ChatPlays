package tv.wlg.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPoolManager {
    private ScheduledExecutorService sequenceExecutor; //Used by vJoy to process sequence
    private ScheduledExecutorService sequenceWorker; //Used by vJoy to submit sequence
    private ScheduledExecutorService chatMessageProcessor; //Used by Chat to Process Messages

    public void setChatMessageProcessor(ScheduledExecutorService chatMessageProcessor) {
        this.chatMessageProcessor = chatMessageProcessor;
    }

    public ScheduledExecutorService getChatMessageProcessor() {
        if (this.chatMessageProcessor == null) {
            this.chatMessageProcessor = Executors.newScheduledThreadPool(10, new NamedThreadFactory("CHAT"));
        }
        return this.chatMessageProcessor;
    }

    public void setSequenceWorker(ScheduledExecutorService sequenceWorker) {
        this.sequenceWorker = sequenceWorker;
    }

    public ScheduledExecutorService getSequenceWorker() {
        if (this.sequenceWorker == null) {
            this.sequenceWorker = Executors.newScheduledThreadPool(1, new NamedThreadFactory("GAMEPAD"));
        }
        return sequenceWorker;
    }

    public void setSequenceExecutor(ScheduledExecutorService sequenceExecutor) {
        this.sequenceExecutor = sequenceExecutor;
    }

    public ScheduledExecutorService getSequenceExecutor() {
        if (this.sequenceExecutor == null) {
            this.sequenceExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("CONSOLE"));
        }

        return this.sequenceExecutor;
    }
}
