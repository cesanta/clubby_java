
package com.cesanta.clubby.lib;

import com.neovisionaries.ws.client.WebSocketException;

/**
 * Generic Clubby listener. Don't confuse with the listener for each particular
 * command's response.
 */
public interface ClubbyListener {

    public void onConnected(Clubby clubby);
    public void onDisconnected(Clubby clubby);

    public void onDataSending(Clubby clubby, String text);
    public void onDataReceived(Clubby clubby, String text);

    public void onConnectError(Clubby clubby, WebSocketException cause);
    public void onError(Clubby clubby, WebSocketException cause);

    public void onStateChanged(Clubby clubby, ClubbyState newState);

    public void handleCallbackError(Clubby clubby, Throwable cause);

}

