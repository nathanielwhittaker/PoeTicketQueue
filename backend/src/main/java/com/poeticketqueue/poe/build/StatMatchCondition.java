package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.item.Stat;

@FunctionalInterface
public interface StatMatchCondition {
    boolean test(String searchKey, String suffix, Stat stat);
}
