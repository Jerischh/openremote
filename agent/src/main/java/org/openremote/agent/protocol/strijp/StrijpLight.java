package org.openremote.agent.protocol.strijp;

public class StrijpLight {
    private String assetId;
    private String host;
    private Integer port;

    public StrijpLight(String assetId, String host, Integer port) {
        this.assetId = assetId;
        this.host = host;
        this.port = port;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
