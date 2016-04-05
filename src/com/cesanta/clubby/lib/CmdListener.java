
package com.cesanta.clubby.lib;

/**
 * Generic listener for the command response. It does not actually contain
 * an `onResponse()` callback, because its signature is different for each
 * command, so, this interface is extended by each command in each service.
 */
public interface CmdListener {
    public void onTimeout() throws ClubbyException;
    public void onError(int status, String status_msg) throws ClubbyException;
}

