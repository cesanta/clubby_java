
package com.cesanta.clubby.lib;

import java.util.ArrayList;
import java.util.List;

import com.neovisionaries.ws.client.WebSocketException;

class ListenerManager {
    private final Clubby clubby;
    private final List<ClubbyListener> listeners
        = new ArrayList<ClubbyListener>();

    public ListenerManager(Clubby clubby) {
        this.clubby = clubby;
    }

    public void addListener(ClubbyListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ClubbyListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void onConnected() {
        for (ClubbyListener listener : listeners) {
            listener.onConnected(clubby);
        }
    }

    public void onDisconnected() {
        for (ClubbyListener listener : listeners) {
            listener.onDisconnected(clubby);
        }
    }

    public void onConnectError(WebSocketException cause) {
        for (ClubbyListener listener : listeners) {
            listener.onConnectError(clubby, cause);
        }
    }

    public void onError(WebSocketException cause) {
        for (ClubbyListener listener : listeners) {
            listener.onError(clubby, cause);
        }
    }

    public void onDataSending(String text) {
        for (ClubbyListener listener : listeners) {
            listener.onDataSending(clubby, text);
        }
    }

    public void onDataReceived(String text) {
        for (ClubbyListener listener : listeners) {
            listener.onDataReceived(clubby, text);
        }
    }

    public void handleCallbackError(Throwable cause) {
        for (ClubbyListener listener : listeners) {
            listener.handleCallbackError(clubby, cause);
        }
    }

    public void onStateChanged(ClubbyState newState) {
        for (ClubbyListener listener : listeners) {
            listener.onStateChanged(clubby, newState);
        }
    }

}
