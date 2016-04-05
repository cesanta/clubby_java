
package com.cesanta.clubby.lib;

import com.neovisionaries.ws.client.WebSocketException;

public class ClubbyAdapter implements ClubbyListener {

    @Override
    public void onConnected(Clubby clubby) {
    }

    @Override
    public void onDisconnected(Clubby clubby) {
    }

    @Override
    public void onDataSending(Clubby clubby, String text) {
    }

    @Override
    public void onDataReceived(Clubby clubby, String text) {
    }

    @Override
    public void onConnectError(Clubby clubby, WebSocketException cause) {
        System.out.println("Connect error: " + cause);
    }

    @Override
    public void onError(Clubby clubby, WebSocketException cause) {
        System.out.println("An error occurred: " + cause);
    }

    @Override
    public void onStateChanged(Clubby clubby, ClubbyState newState) {
    }

    @Override
    public void handleCallbackError(Clubby clubby, Throwable cause) {
        /*
         * Not that we can do much about the error; but since all the
         * methods in `WebSocketListener` throw `Exception`, we have to
         * treat them somehow. Here, we treat any exception as a fatal
         * error.
         *
         * All recoverable cases should be handled individually in each
         * particular callback.
         */

        cause.printStackTrace();
        System.exit(1);
    }

}

