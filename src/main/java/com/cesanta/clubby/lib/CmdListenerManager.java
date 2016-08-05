/*
 * Copyright (c) 2016 Cesanta Software Limited
 * All rights reserved
 */

package com.cesanta.clubby.lib;

import java.util.HashMap;
import java.util.Map;

final class CmdListenerManager {

    private final Map<Integer, CmdListenerWrapper<?>> listeners =
        new HashMap<Integer, CmdListenerWrapper<?>>();

    public synchronized void addCmdListener(CmdListenerWrapper<?> listenerWrapper) {
        if (listenerWrapper != null) {
            listeners.put(listenerWrapper.getCmdId(), listenerWrapper);
        }
    }

    public synchronized CmdListenerWrapper<?> popListener(int id) {
        CmdListenerWrapper<?> ret = listeners.get(id);
        listeners.remove(id);
        return ret;
    }
}

