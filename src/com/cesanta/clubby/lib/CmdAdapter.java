
package com.cesanta.clubby.lib;

/**
 * Default implentation of CmdListener, parametrized with the response type.
 *
 * If you don't care about the response data, just use `Object`.
 */
public class CmdAdapter<R> implements CmdListener<R> {

    @Override
    public void onResponse(R response) {}

    @Override
    public void onTimeout() throws ClubbyException {
        throw new ClubbyException("timeout");
    }

    @Override
    public void onError(int status, String status_msg) throws ClubbyException {
        throw new ClubbyException(status_msg);
    }
}

