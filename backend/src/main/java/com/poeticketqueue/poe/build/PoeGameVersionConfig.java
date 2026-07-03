package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;

import java.util.List;

public interface PoeGameVersionConfig {
    String getLeague();
    String getStatsApiUrl();
    String getItemsApiUrl();
    BaseTypeApiCategoryProperties getBaseTypeCategoryProperties();
    WeaponBaseStatsProperties getWeaponBaseStats();
    PathOfExileTradeApi getTradeApi();
    List<Stat> getTradeStats();
    List<Item> getTradeBaseTypesWithLocalMods();
    List<Item> getAllBaseTypes();
}
