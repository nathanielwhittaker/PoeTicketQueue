package com.poeticketqueue.poe.item;

import com.poeticketqueue.poe.api.BaseTypeApiCategoryProperties;
import com.poeticketqueue.poe.api.PathOfExileTradeApi;
import com.poeticketqueue.poe.api.WeaponBaseStats;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.build.Build;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEST-FIRST, network-free, Spring-free contract tests for {@link Item#fromStringForBuild(String, Build)}.
 *
 * <p>These lock in the CONFIRMED design for build-import stat parsing:
 * <ul>
 *   <li>By default (build.isUseTrueValues() == false), single-value stats are scaled by
 *       {@code statRollDelta} (0.85) but keep the value's ORIGINAL position in the text as a
 *       {@code #} placeholder (e.g. {@code "+21 to maximum Life"} -&gt; text {@code "+# to maximum Life"}).</li>
 *   <li>Non-weapon "Adds X to Y ... Damage" stats are shown as a full min-max RANGE
 *       (en dash, e.g. {@code "Adds 10–20 Fire Damage"}), get NO trade upper bound
 *       ({@code getMaxRoll() == -1}), and use the scaled average as the roll.</li>
 *   <li>When build.isUseTrueValues() == true, scaling is bypassed entirely (raw value / raw average),
 *       but the displayed text shape is unchanged.</li>
 *   <li>On a weapon base type, "Adds X to Y ... Damage" lines are consumed into DPS computation
 *       (item.pdps / item.edps) and do NOT appear as a stat row.</li>
 *   <li>Stats matching a configured roll-delta exclusion (e.g. "to level of") always use the raw,
 *       unscaled integer roll, regardless of the useTrueValues toggle.</li>
 * </ul>
 *
 * <p>These were authored test-first (red) against the locked design and now pass against the
 * implemented parser: {@code Build.isUseTrueValues()} drives the scaling bypass, and
 * {@link Item#fromStringForBuild} builds the range/placeholder text shapes described above.
 */
class ItemBuildParseTest {

    private static Stat findStatContaining(Item item, String textFragment) {
        List<Stat> matches = item.getStats().stream()
                .filter(s -> s.getText() != null && s.getText().contains(textFragment))
                .toList();
        assertThat(matches)
                .as("expected exactly one stat containing '%s' but found %d: %s", textFragment, matches.size(), matches)
                .hasSize(1);
        return matches.get(0);
    }

    /**
     * Minimal, network-free stub of the abstract {@link Build} class.
     *
     * <p>{@code Build}'s no-arg constructor calls {@code getTradeStats()} and
     * {@code getTradeBaseTypesWithLocalMods()}, so both must return a non-null (empty) list here.
     *
     * <p>{@code getIdFromStatText(...)} is overridden directly (rather than relying on the real
     * {@code allPossibleStats}-based lookup) so that any non-empty stat search key resolves to a
     * canned, non-null id -- letting tests assert {@code getId() != null} without needing a real
     * trade-stats catalog.
     */
    private static class StubBuild extends Build {

        private final boolean useTrueValues;
        private final boolean weapon;

        StubBuild(boolean useTrueValues, boolean weapon) {
            this.useTrueValues = useTrueValues;
            this.weapon = weapon;
        }

        // NOTE: Build.isUseTrueValues() does not exist yet -- this @Override is the intended
        // compile-RED anchor for this whole test file.
        @Override
        public boolean isUseTrueValues() {
            return useTrueValues;
        }

        @Override
        public String getName() { return "stub"; }

        @Override
        public String getLeague() { return ""; }

        @Override
        public String getStatsApiUrl() { return ""; }

        @Override
        public String getItemsApiUrl() { return ""; }

        @Override
        public PathOfExileTradeApi getTradeApi() { return null; }

        @Override
        public List<Stat> getTradeStats() { return List.of(); }

        @Override
        public List<Item> getTradeBaseTypesWithLocalMods() { return List.of(); }

        @Override
        public List<Item> getAllBaseTypes() { return List.of(); }

        @Override
        public BaseTypeApiCategoryProperties getBaseTypeCategoryProperties() {
            if (!weapon) {
                // Resource "__none__" does not exist -> empty category map -> getCategory() returns
                // null for any base type -> Item.computeIsWeapon(...) treats it as non-weapon.
                return new BaseTypeApiCategoryProperties("__none__");
            }
            return new BaseTypeApiCategoryProperties("__none__") {
                @Override
                public String getCategory(String baseType) {
                    return "weapon.sword";
                }
            };
        }

        @Override
        public WeaponBaseStatsProperties getWeaponBaseStats() {
            if (!weapon) {
                // computeIsWeapon(...) returns false first for non-weapon cases, so this value is
                // never consulted -- null is fine.
                return null;
            }
            return new WeaponBaseStatsProperties("__none__") {
                @Override
                public WeaponBaseStats getStats(String baseType) {
                    return new WeaponBaseStats(10, 20, 1.5);
                }
            };
        }

        @Override
        public String getIdFromStatText(String statText) {
            return (statText != null && !statText.isEmpty()) ? "explicit.stat_test" : null;
        }

        @Override
        public String getIdFromStatText(String statText, String idPrefix) {
            return (statText != null && !statText.isEmpty()) ? "explicit.stat_test" : null;
        }
    }

    private static StubBuild nonWeaponBuild(boolean useTrueValues) {
        return new StubBuild(useTrueValues, false);
    }

    private static StubBuild weaponBuild() {
        return new StubBuild(false, true);
    }

    @Test
    void singleValueStat_defaultScaling_showsValueInOriginalPosition_androlledByStatRollDelta() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\n+21 to maximum Life";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(false));

        Stat stat = findStatContaining(item, "maximum Life");
        assertThat(stat.getText()).isEqualTo("+# to maximum Life");
        assertThat(stat.getRoll()).isEqualTo(17); // (int) (21 * 0.85)
    }

    @Test
    void nonWeaponAddedFlatDamage_showsFullMinMaxRange_withNoTradeUpperBound() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\nAdds 10 to 20 Fire Damage";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(false));

        Stat stat = findStatContaining(item, "Fire Damage");
        assertThat(stat.getText()).isEqualTo("Adds 10–20 Fire Damage"); // en dash, no '#'
        assertThat(stat.getText()).doesNotContain("#");
        assertThat(stat.getMaxRoll()).isEqualTo(-1); // trade search must NOT get an upper bound
        assertThat(stat.getRoll()).isEqualTo(12); // (int) (((10 + 20) / 2.0) * 0.85) == (int) (15 * 0.85)
        assertThat(stat.getId()).isNotNull();
    }

    @Test
    void trueValuesToggle_bypassesScaling_forSingleValueStat() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\n+21 to maximum Life";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(true));

        Stat stat = findStatContaining(item, "maximum Life");
        assertThat(stat.getText()).isEqualTo("+# to maximum Life");
        assertThat(stat.getRoll()).isEqualTo(21); // unscaled
    }

    @Test
    void trueValuesToggle_bypassesScaling_forAddedFlatDamageRange() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\nAdds 10 to 20 Fire Damage";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(true));

        Stat stat = findStatContaining(item, "Fire Damage");
        assertThat(stat.getText()).isEqualTo("Adds 10–20 Fire Damage");
        assertThat(stat.getRoll()).isEqualTo(15); // true average, unscaled: (10 + 20) / 2.0
        assertThat(stat.getMaxRoll()).isEqualTo(-1);
    }

    @Test
    void weaponAddedFlatDamage_feedsDps_andIsNotShownAsAStatRow() {
        String block = "Rarity: RARE\nTest Blade\nWar Sword\nAdds 5 to 9 Physical Damage";

        Item item = Item.fromStringForBuild(block, weaponBuild());

        assertThat(item.getStats())
                .noneMatch(s -> s.getText() != null && s.getText().contains("Physical Damage"));
        assertThat(item.pdps).isGreaterThan(0);
    }

    @Test
    void exclusionBranch_usesRawUnscaledRoll_whenUseTrueValuesIsFalse() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\n+3 to Level of all Fire Skill Gems";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(false));

        Stat stat = findStatContaining(item, "Level of all Fire Skill Gems");
        assertThat(stat.getRoll()).isEqualTo(3);
    }

    @Test
    void exclusionBranch_usesRawUnscaledRoll_whenUseTrueValuesIsTrue() {
        String block = "Rarity: RARE\nTest Ring\nCoral Ring\n+3 to Level of all Fire Skill Gems";

        Item item = Item.fromStringForBuild(block, nonWeaponBuild(true));

        Stat stat = findStatContaining(item, "Level of all Fire Skill Gems");
        assertThat(stat.getRoll()).isEqualTo(3);
    }
}
