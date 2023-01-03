package com.sokima.monopoly.event;

import com.sokima.monopoly.dto.deck.event.EventCell;
import com.sokima.monopoly.dto.player.Businessman;

public interface Event {

    /**
     * an event to happen when you land on {@link EventCell}
     *
     * @param player the {@link Businessman}
     */
    void onEvent(Businessman player);

    /**
     * print message after event
     */
    void printMessage();
}