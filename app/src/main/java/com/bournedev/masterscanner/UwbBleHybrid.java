package com.bournedev.masterscanner;

public class UwbBleHybrid {

    public String tag;
    public String trackedDevice;
    public String rssi;

    public UwbBleHybrid() {
    }

    public UwbBleHybrid(String tag, String trackedDevice, String rssi) {
        this.tag = tag;
        this.trackedDevice = trackedDevice;
        this.rssi = rssi;
    }
}
