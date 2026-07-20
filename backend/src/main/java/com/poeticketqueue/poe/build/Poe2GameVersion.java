package com.poeticketqueue.poe.build;

public interface Poe2GameVersion extends PoeVersionDelegatingGameVersion {
    @Override default PoeVersion poeVersion() { return PoeVersion.POE2; }
}
