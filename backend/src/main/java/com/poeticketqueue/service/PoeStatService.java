package com.poeticketqueue.service;

import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoeStatService {

    private static final Logger log = LoggerFactory.getLogger(PoeStatService.class);

    public List<Stat> getStats(PoeVersion version) {
        return version.getTradeStats();
    }

    public List<Item> getAllBaseTypes(PoeVersion version) {
        return version.getAllBaseTypes();
    }

    public List<Item> getBaseTypesWithLocalMods(PoeVersion version) {
        return version.getTradeBaseTypesWithLocalMods();
    }
}
