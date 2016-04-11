
package com.cesanta.clubby.lib;

import com.neovisionaries.ws.client.WebSocketException;

/**
 * Generic Clubby listener. Don't confuse with the listener for each particular
 * command's response.
 */
public interface ClubbyListener {

    /**
     * Called whenever the websocket is connected to the server successfully.
     *
     * @see Clubby#connect()
     */
    public void onConnected(Clubby clubby);

    /**
     * Called whenever the websocket connection fails.
     *
     * @see Clubby#connect()
     */
    public void onConnectError(Clubby clubby, WebSocketException cause);

    /**
     * Called whenever the websocket is disconnected from the server
     *
     * @see Clubby#disconnect()
     */
    public void onDisconnected(Clubby clubby);

    /**
     * Called just before sending raw data on the wire.
     */
    public void onDataSending(Clubby clubby, String text);

    /**
     * Called just after new raw data was received from the wire.
     */
    public void onDataReceived(Clubby clubby, String text);

    public void onError(Clubby clubby, WebSocketException cause);

    public void onStateChanged(Clubby clubby, ClubbyState newState);

    public void handleCallbackError(Clubby clubby, Throwable cause);

}

