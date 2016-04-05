
package com.cesanta.clubby.lib;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class CmdListenerWrapper {

    /*
     * NOTE: will be hidden in subclasses
     */
    protected CmdListener listener;

    private int cmdId;

    protected abstract void onResponseGeneric(ObjectMapper mapper, String respStr) throws IOException;

    void setCmdId(int cmdId) {
        this.cmdId = cmdId;
    }

    int getCmdId() {
        return this.cmdId;
    }

}

