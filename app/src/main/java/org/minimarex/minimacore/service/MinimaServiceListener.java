package org.minimarex.minimacore.service;

public interface MinimaServiceListener {
    public void MinimaServiceShutdown();

    public void MinimaNewBlock();

    public void MinimaLoadKeys(int zKeys, boolean zFinished);

}
