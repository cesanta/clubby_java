
package com.cesanta.clubby.lib;

/**
 * Default implentation of CmdListener
 */
public class CmdAdapter implements CmdListener {

    @Override
    public void onTimeout() throws ClubbyException {
        throw new ClubbyException("timeout");
    }

    @Override
    public void onError(int status, String status_msg) throws ClubbyException {
        throw new ClubbyException(status_msg);
    }
}

