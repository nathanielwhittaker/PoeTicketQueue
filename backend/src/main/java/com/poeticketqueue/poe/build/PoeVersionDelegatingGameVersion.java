package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;

import java.util.List;

public interface PoeVersionDelegatingGameVersion extends PoeGameVersionConfig {

    PoeVersion poeVersion();

    @Override default String getLeague()                                               { return poeVersion().getLeague(); }
    @Override default String getStatsApiUrl()                                          { return poeVersion().getStatsApiUrl(); }
    @Override default String getItemsApiUrl()                                          { return poeVersion().getItemsApiUrl(); }
    @Override default BaseTypeApiCategoryProperties getBaseTypeCategoryProperties()    { return poeVersion().getBaseTypeCategoryProperties(); }
    @Override default WeaponBaseStatsProperties getWeaponBaseStats()                   { return poeVersion().getWeaponBaseStats(); }
    @Override default PathOfExileTradeApi getTradeApi()                                { return poeVersion().getTradeApi(); }
    @Override default List<Stat> getTradeStats()                                       { return poeVersion().getTradeStats(); }
    @Override default List<Item> getTradeBaseTypesWithLocalMods()                      { return poeVersion().getTradeBaseTypesWithLocalMods(); }
    @Override default List<Item> getAllBaseTypes()                                      { return poeVersion().getAllBaseTypes(); }
}
