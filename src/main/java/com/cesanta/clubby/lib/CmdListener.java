
package com.cesanta.clubby.lib;

/**
 * Generic listener for the command response, parametrized with the response
 * type.
 *
 * If you don't care about the response data, just use `Object`.
 */
public interface CmdListener<R> {

    /**
     * Called when a successful response (with zero status) has been received.
     */
    public void onResponse(R response);

    /**
     * Called when a non-successful response (with non-zero status) has been
     * received.
     */
    public void onError(int status, String statusMsg);

}

