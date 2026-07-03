package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;

import java.util.List;

public interface Poe2GameVersion extends PoeGameVersionConfig {

    @Override default String getLeague()                                               { return PoeVersion.POE2.getLeague(); }
    @Override default String getStatsApiUrl()                                          { return PoeVersion.POE2.getStatsApiUrl(); }
    @Override default String getItemsApiUrl()                                          { return PoeVersion.POE2.getItemsApiUrl(); }
    @Override default BaseTypeApiCategoryProperties getBaseTypeCategoryProperties()    { return PoeVersion.POE2.getBaseTypeCategoryProperties(); }
    @Override default WeaponBaseStatsProperties getWeaponBaseStats()                   { return PoeVersion.POE2.getWeaponBaseStats(); }
    @Override default PathOfExileTradeApi getTradeApi()                                { return PoeVersion.POE2.getTradeApi(); }
    @Override default List<Stat> getTradeStats()                                       { return PoeVersion.POE2.getTradeStats(); }
    @Override default List<Item> getTradeBaseTypesWithLocalMods()                      { return PoeVersion.POE2.getTradeBaseTypesWithLocalMods(); }
    @Override default List<Item> getAllBaseTypes()                                      { return PoeVersion.POE2.getAllBaseTypes(); }
}
