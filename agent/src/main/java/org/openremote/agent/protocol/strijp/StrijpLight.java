package org.openremote.agent.protocol.strijp;

public class StrijpLight {
    private String host;

    public StrijpLight(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
