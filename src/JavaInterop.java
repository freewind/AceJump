import com.intellij.util.EventDispatcher;

import javax.swing.event.ChangeListener;

public class JavaInterop {
    public static EventDispatcher<ChangeListener> createChangeListener() {
        return EventDispatcher.create(ChangeListener.class);
    }
}
