/*
 * Copyright (c) 2016 Cesanta Software Limited
 * All rights reserved
 */

package com.cesanta.clubby.lib;

import java.io.IOException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper for the user-provided command listener.
 *
 * NOTE: the only valid use of ListenerWrapper is to create it, initialize with
 * parameters, and save into CmdListenerManager. You should never use it
 * directly to notify the listener; the only way to get ListenerWrapper
 * instance back is to call the popListener() method of CmdListenerManager,
 * because listeners can be added and popped concurrently, and
 * CmdListenerManager handles it.
 *
 * Methods of the wrapper itself are not synchronized, because only one thread
 * can get each particular listener.
 */
final class CmdListenerWrapper<R> {

    private CmdListener<R> listener;
    private Class<R> cls;

    private int cmdId;
    private Future<?> timeoutFuture = null;

    CmdListenerWrapper(CmdListener<R> listener, Class<R> cls) {
        this.listener = listener;
        this.cls = cls;
    }

    protected void onResponseGeneric(
            ObjectMapper mapper, String respJsonStr
            ) throws IOException  {
        cancelTimeout();
        this.listener.onResponse(
                mapper.readValue(
                    respJsonStr, cls
                    )
                );
    }

    protected void onError(int status, String statusMsg) {
        cancelTimeout();
        this.listener.onError(status, statusMsg);
    }

    void setCmdId(int cmdId) {
        this.cmdId = cmdId;
    }

    void setTimeoutFuture(Future<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }

    int getCmdId() {
        return this.cmdId;
    }

    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

}

