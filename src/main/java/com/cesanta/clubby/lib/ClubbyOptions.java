
package com.cesanta.clubby.lib;

public final class ClubbyOptions {

    private int timeout = 0;

    private ClubbyOptions() {}

    public static ClubbyOptions createDefault() {
        return new ClubbyOptions();
    }

    public static ClubbyOptions createFrom(ClubbyOptions src) {
        ClubbyOptions ret = new ClubbyOptions();
        ret.timeout(src.getTimeout());
        return ret;
    }

    /**
     * Set number of seconds after when the command result is no longer
     * relevant (and the {@link CmdListener#onError(int, String) onError()}
     * method of the listener will be called). Set 0 for no timeout.
     * Default: 0.
     */
    public final ClubbyOptions timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public final int getTimeout() {
        return timeout;
    }

}

