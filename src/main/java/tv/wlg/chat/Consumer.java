package tv.wlg.chat;

@FunctionalInterface
public interface Consumer {
    public void process(Message message);
}
