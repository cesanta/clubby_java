/*
 * Copyright (c) 2016 Cesanta Software Limited
 * All rights reserved
 */

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
    public void onError(int status, String statusMsg) {
        throw new ClubbyException(statusMsg);
    }
}

