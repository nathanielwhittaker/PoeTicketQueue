package com.poeticketqueue.poe.build;

public interface Poe1GameVersion extends PoeVersionDelegatingGameVersion {
    @Override default PoeVersion poeVersion() { return PoeVersion.POE1; }
}
