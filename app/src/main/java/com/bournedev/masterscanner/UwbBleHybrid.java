package com.bournedev.masterscanner;

public class UwbBleHybrid {

    public String trackedDevice;
    public String rssi;

    public UwbBleHybrid() {
    }

    public UwbBleHybrid(String trackedDevice, String rssi) {
        this.trackedDevice = trackedDevice;
        this.rssi = rssi;
    }
}
