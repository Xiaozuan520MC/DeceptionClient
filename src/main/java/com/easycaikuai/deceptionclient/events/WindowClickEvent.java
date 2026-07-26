package com.easycaikuai.deceptionclient.events;

import com.easycaikuai.deceptionclient.event.events.callables.EventCancellable;

public class WindowClickEvent extends EventCancellable {
    private final int windowsId;
    private final int slotId;
    private final int mouseButtonClicked;
    private final int mode;

    public WindowClickEvent(int windowsId, int slotId, int mouseButtonClicked, int mode) {
        this.windowsId = windowsId;
        this.slotId = slotId;
        this.mouseButtonClicked = mouseButtonClicked;
        this.mode = mode;
    }
}
