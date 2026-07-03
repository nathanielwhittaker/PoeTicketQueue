package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;

import java.util.List;

public interface Poe1GameVersion extends PoeGameVersionConfig {

    @Override default String getLeague()                                               { return PoeVersion.POE1.getLeague(); }
    @Override default String getStatsApiUrl()                                          { return PoeVersion.POE1.getStatsApiUrl(); }
    @Override default String getItemsApiUrl()                                          { return PoeVersion.POE1.getItemsApiUrl(); }
    @Override default BaseTypeApiCategoryProperties getBaseTypeCategoryProperties()    { return PoeVersion.POE1.getBaseTypeCategoryProperties(); }
    @Override default WeaponBaseStatsProperties getWeaponBaseStats()                   { return PoeVersion.POE1.getWeaponBaseStats(); }
    @Override default PathOfExileTradeApi getTradeApi()                                { return PoeVersion.POE1.getTradeApi(); }
    @Override default List<Stat> getTradeStats()                                       { return PoeVersion.POE1.getTradeStats(); }
    @Override default List<Item> getTradeBaseTypesWithLocalMods()                      { return PoeVersion.POE1.getTradeBaseTypesWithLocalMods(); }
    @Override default List<Item> getAllBaseTypes()                                      { return PoeVersion.POE1.getAllBaseTypes(); }
}
