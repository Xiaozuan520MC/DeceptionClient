package com.easycaikuai.deceptionclient.events;

import com.easycaikuai.deceptionclient.event.events.Event;

public class KeyEvent implements Event {
    private final int keyCode;

    public KeyEvent(int key) {
        this.keyCode = key;
    }

    public int getKey() {
        return this.keyCode;
    }
}
