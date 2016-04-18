
package com.cesanta.clubby.lib;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class CmdListenerWrapper<R> {

    protected CmdListener<R> listener;
    protected Class<R> cls;

    private int cmdId;

    CmdListenerWrapper(CmdListener<R> listener, Class<R> cls) {
        this.listener = listener;
        this.cls = cls;
    }

    protected void onResponseGeneric(
            ObjectMapper mapper, String respJsonStr
            ) throws IOException  {
        this.listener.onResponse(
                mapper.readValue(
                    respJsonStr, cls
                    )
                );
    }

    void setCmdId(int cmdId) {
        this.cmdId = cmdId;
    }

    int getCmdId() {
        return this.cmdId;
    }

}

