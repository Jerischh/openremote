package org.openremote.agent.protocol.strijp;

public class StrijpLight {
    private String assetId;
    private String host;
    private Integer port;
    private double x;
    private double y;

    StrijpLight(String assetId, String host, Integer port) {
        this.assetId = assetId;
        this.host = host;
        this.port = port;
    }

    StrijpLight(String assetId, String host, Integer port, double x, double y) {
        this.assetId = assetId;
        this.host = host;
        this.port = port;
        this.x = x;
        this.y = y;
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
