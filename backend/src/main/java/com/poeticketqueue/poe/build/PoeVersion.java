package com.poeticketqueue.poe.build;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExile1TradeApi;
import com.poeticketqueue.poe.api.PathOfExile2TradeApi;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.config.PropertiesManagerCore;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.poe.item.Stat;

import java.util.List;

public enum PoeVersion implements PoeGameVersionConfig {

    POE1 {
        private final BaseTypeApiCategoryProperties categories =
                new BaseTypeApiCategoryProperties("PathOfExile1BaseTypeApiCategory.properties");
        private final WeaponBaseStatsProperties weaponStats =
                new WeaponBaseStatsProperties("PathOfExile1WeaponBaseStats.properties");
        private final PathOfExileTradeApi tradeApi = new PathOfExile1TradeApi();

        @Override public String getLeague()      { return PropertiesManagerCore.getLeague(); }
        @Override public String getStatsApiUrl() { return PropertiesManagerCore.getStatsApiLink(); }
        @Override public String getItemsApiUrl() { return PropertiesManagerCore.getItemsApiLink(); }
        @Override public BaseTypeApiCategoryProperties getBaseTypeCategoryProperties() { return categories; }
        @Override public WeaponBaseStatsProperties getWeaponBaseStats()               { return weaponStats; }
        @Override public PathOfExileTradeApi getTradeApi()                            { return tradeApi; }
        @Override public List<Stat> getTradeStats()                                   { return tradeApi.getStats(); }
        @Override public List<Item> getTradeBaseTypesWithLocalMods()                  { return tradeApi.getBaseTypesWithLocalMods(); }
        @Override public List<Item> getAllBaseTypes()                                  { return tradeApi.getAllBaseTypes(); }
    },

    POE2 {
        private final BaseTypeApiCategoryProperties categories =
                new BaseTypeApiCategoryProperties("PathOfExile2BaseTypeApiCategory.properties");
        private final WeaponBaseStatsProperties weaponStats =
                new WeaponBaseStatsProperties("PathOfExile2WeaponBaseStats.properties");
        private final PathOfExileTradeApi tradeApi = new PathOfExile2TradeApi();

        @Override public String getLeague()      { return PropertiesManagerCore.getLeaguePoe2(); }
        @Override public String getStatsApiUrl() { return PropertiesManagerCore.getStatsApiLinkPoe2(); }
        @Override public String getItemsApiUrl() { return PropertiesManagerCore.getItemsApiLinkPoe2(); }
        @Override public BaseTypeApiCategoryProperties getBaseTypeCategoryProperties() { return categories; }
        @Override public WeaponBaseStatsProperties getWeaponBaseStats()               { return weaponStats; }
        @Override public PathOfExileTradeApi getTradeApi()                            { return tradeApi; }
        @Override public List<Stat> getTradeStats()                                   { return tradeApi.getStats(); }
        @Override public List<Item> getTradeBaseTypesWithLocalMods()                  { return tradeApi.getBaseTypesWithLocalMods(); }
        @Override public List<Item> getAllBaseTypes()                                  { return tradeApi.getAllBaseTypes(); }
    };
}
