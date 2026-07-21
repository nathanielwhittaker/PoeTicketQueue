package com.poeticketqueue.poe.item;

import com.poeticketqueue.poe.api.Flags;
import com.poeticketqueue.poe.api.WeaponBaseStats;
import com.poeticketqueue.poe.api.WeaponBaseStatsProperties;
import com.poeticketqueue.poe.build.Build;
import com.poeticketqueue.poe.config.PropertiesManagerCore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Item implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Item.class);

    @Serial
    private static final long serialVersionUID = 2456L;

    public String name;
    public List<Stat> stats;
    public List<StatGroup> statGroups;
    public String type;
    public String text;
    public Flags flags;
    public String baseType;
    public String rarity;
    public String queuedBy;
    public int es;
    public int evasion;
    public int armour;
    public int ward;
    public int block;
    public int basePercentile;
    public int maxArmour;
    public int maxEvasion;
    public int maxEs;
    public int maxWard;
    public int maxBlock;
    public int maxBasePercentile;
    public int edps;
    public int pdps;
    public int maxEdps;
    public int maxPdps;
    public int maxLocalCrit;
    public int aps;
    public int maxAps;
    public int damage;
    public int maxDamage;
    public int links;
    public int ilvl;
    public Boolean corrupted;
    public boolean foulborn;
    public int gemLevel;
    public int localBaseCrit;
    public int quality;
    public Integer id;

    public static final Map<String, ItemDefenseSetter> defenseSetterMap = Map.of(
            "energy shield:", (i, v) -> i.es = v,
            "evasion:",       (i, v) -> i.evasion = v,
            "armour:",        (i, v) -> i.armour = v
    );

    private record ParsedStat(String stat, String displayText, String searchKey, double numericRoll) {}
    private record StatResolution(String id, double roll) {}

    // For deserialization only
    public Item() {}

    public Item(String name, String rarity, String type, List<Stat> stats) {
        this(name, rarity, type, null, stats, -1, -1, -1, -1, -1, -1, -1, null, -1, -1);
    }

    public Item(String name, String rarity, String type, String baseType, List<Stat> stats,
                int es, int evasion, int armour, int edps, int pdps,
                int ilvl, int links, Boolean corrupted, int gemLevel, int localBaseCrit) {
        this.name = name;
        this.type = type;
        this.rarity = rarity;
        this.stats = stats;
        this.es = es;
        this.evasion = evasion;
        this.armour = armour;
        this.baseType = baseType;
        this.edps = edps;
        this.pdps = pdps;
        this.ilvl = ilvl;
        this.links = links;
        this.corrupted = corrupted;
        this.gemLevel = gemLevel;
        this.localBaseCrit = localBaseCrit;
    }

    public Item(String name, String rarity, String baseType) {
        this.name = name;
        this.rarity = rarity;
        this.baseType = baseType;
        this.stats = new ArrayList<>();
    }

    public String getName()                            { return name; }
    public List<Stat> getStats()                       { return stats; }
    public void setStats(List<Stat> stats)             { this.stats = stats; }
    public List<StatGroup> getStatGroups()             { return statGroups; }
    public void setStatGroups(List<StatGroup> groups)  { this.statGroups = groups; }
    public void setType(String type)       { this.type = type; }
    public String getType()        { return type; }
    public String getText()        { return text; }
    public Flags getFlags()        { return flags; }
    public void setFlags(Flags flags) { this.flags = flags; }
    public String getRarity()      { return rarity; }
    public String getQueuedBy()    { return queuedBy; }
    public int getEs()             { return es; }
    public int getEvasion()        { return evasion; }
    public int getArmour()         { return armour; }
    public String getBaseType()    { return baseType; }
    public int getEDPS()           { return edps; }
    public int getPDPS()           { return pdps; }
    public int getIlvl()           { return ilvl; }
    public int getLinks()          { return links; }
    public Boolean getCorrupted()   { return corrupted; }
    public boolean isFoulborn()     { return foulborn; }
    public int getGemLevel()        { return gemLevel; }
    public int getLocalCrit()       { return localBaseCrit; }
    public int getWard()                { return ward; }
    public int getBlock()               { return block; }
    public int getBasePercentile()      { return basePercentile; }
    public int getMaxArmour()           { return maxArmour; }
    public int getMaxEvasion()          { return maxEvasion; }
    public int getMaxEs()               { return maxEs; }
    public int getMaxWard()             { return maxWard; }
    public int getMaxBlock()            { return maxBlock; }
    public int getMaxBasePercentile()   { return maxBasePercentile; }
    public int getMaxPDPS()             { return maxPdps; }
    public int getMaxEDPS()             { return maxEdps; }
    public int getMaxLocalCrit()        { return maxLocalCrit; }
    public int getAPS()                 { return aps; }
    public int getMaxAPS()              { return maxAps; }
    public int getDamage()              { return damage; }
    public int getMaxDamage()           { return maxDamage; }

    public static Item fromStringForBuild(String item, Build build) {
        String[] statLines = item.split("\n");
        String rarity = statLines[0].substring("Rarity: ".length());
        String baseType = null;
        String name = statLines[1];
        boolean foulborn = false;
        if (name.toLowerCase().startsWith("foulborn ")) {
            name = name.substring("foulborn ".length());
            foulborn = true;
        }
        List<String> extraStatsToIgnore = List.of(
                "to maximum energy shield",
                "increased energy shield",
                "increased armour, evasion and energy shield",
                "to evasion rating",
                "increased evasion rating",
                "to armour",
                "increased armour"
        );
        int startStatParseIndex = 2;
        if (!rarity.equalsIgnoreCase("MAGIC")) {
            startStatParseIndex = 3;
            baseType = statLines[2];
            if (rarity.equalsIgnoreCase("UNIQUE")
                    && !PropertiesManagerCore.getUniqueItemWhitelist().contains(name)
                    && !foulborn) {
                return new Item(name, rarity, baseType);
            }
        } else {
            String magicNameLower = name.toLowerCase();
            baseType = build.getAllBaseTypes().stream()
                    .map(Item::getBaseType)
                    .filter(bt -> bt != null && magicNameLower.contains(bt.toLowerCase()))
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            if (baseType == null) log.warn("Could not determine base type for magic item '{}'", name);
        }

        Item finalItem = new Item(name, rarity, baseType);
        finalItem.foulborn = foulborn;
        List<Stat> allStatsOnThisItem = new ArrayList<>();

        int implicitsStartIndex = -1;
        int implicitCount = 0;
        Set<String> runeStatPatterns = new HashSet<>();
        Map<String, List<String>> runeStatsMap = PropertiesManagerCore.getRuneStatsMap();
        for (int i = startStatParseIndex; i < statLines.length; i++) {
            String lineLower = statLines[i].toLowerCase();
            if (lineLower.startsWith("rune:")) {
                String runeName = statLines[i].substring("Rune:".length()).trim().toLowerCase();
                List<String> patterns = runeStatsMap.get(runeName);
                if (patterns != null) runeStatPatterns.addAll(patterns);
            }
            if (lineLower.startsWith("implicits:")) {
                try { implicitCount = Integer.parseInt(statLines[i].split(":")[1].trim()); } catch (NumberFormatException ignored) {}
                implicitsStartIndex = i + 1;
                break;
            }
        }

        boolean isWeapon = computeIsWeapon(baseType, build);
        Map<String, Double> weaponDmg = new LinkedHashMap<>();
        Set<Integer> skipLines = new HashSet<>();

        for (int i = startStatParseIndex; i < statLines.length; i++) {
            if (skipLines.contains(i)) continue;
            if (applyStructuredField(statLines[i], finalItem)) continue;

            ParsedStat ps = parseStat(statLines[i], build.isUseTrueValues());

            if (isWeapon && Stat.weaponDpsStatNames.contains(ps.stat().toLowerCase().trim())) {
                weaponDmg.merge(ps.stat().toLowerCase().trim(), ps.numericRoll(), Double::sum);
                continue;
            }
            if (extraStatsToIgnore.contains(ps.stat().toLowerCase().trim())) continue;

            if (implicitsStartIndex >= 0 && i >= implicitsStartIndex && i < implicitsStartIndex + implicitCount) {
                if (!runeStatPatterns.isEmpty()) {
                    String finalKey = ps.searchKey();
                    if (runeStatPatterns.stream().anyMatch(p -> finalKey.contains(p.toLowerCase()))) continue;
                }
                allStatsOnThisItem.add(resolveImplicitStat(ps, build));
            } else {
                StatResolution resolution = resolveStatId(ps.searchKey(), ps.numericRoll(), i, statLines, skipLines, build);
                if (resolution.id() == null) log.warn("No trade stat ID found for '{}' on item '{}'", ps.searchKey(), name);
                allStatsOnThisItem.add(new Stat(ps.displayText(), resolution.id(), resolution.roll()));
            }
        }

        if (isWeapon) computeAndApplyWeaponDps(finalItem, baseType, build, weaponDmg);
        finalItem.setStats(collapseStats(allStatsOnThisItem));
        return finalItem;
    }

    private static boolean computeIsWeapon(String baseType, Build build) {
        if (baseType != null) {
            String cat = build.getBaseTypeCategoryProperties().getCategory(baseType);
            return cat != null && cat.startsWith("weapon.");
        }
        return false;
    }

    private static boolean applyStructuredField(String line, Item item) {
        if (line.toLowerCase().startsWith("item level:")) {
            try { item.ilvl = Integer.parseInt(line.split(":")[1].trim()); } catch (NumberFormatException ignored) {}
            return true;
        }
        if (line.toLowerCase().startsWith("sockets:")) {
            String sockStr = line.substring(line.indexOf(':') + 1).trim();
            int maxLinks = 0;
            for (String group : sockStr.split("\\s+")) maxLinks = Math.max(maxLinks, group.split("-").length);
            item.links = maxLinks;
            return true;
        }
        if (line.toLowerCase().startsWith("level:")) {
            try { item.gemLevel = Integer.parseInt(line.split(":")[1].trim()); } catch (NumberFormatException ignored) {}
            return true;
        }
        if (line.trim().equalsIgnoreCase("corrupted"))          { item.corrupted = true; return true; }
        if (line.toLowerCase().startsWith("implicits:"))        return true;
        if (line.toLowerCase().startsWith("bonded:"))           return true;
        if (line.toLowerCase().startsWith("quality:")) {
            try { item.quality = Integer.parseInt(line.split(":")[1].trim()); } catch (NumberFormatException ignored) {}
            return true;
        }
        if (line.length() > 4 && Stat.statStringsToIgnoreWhenParsing.contains(line.toLowerCase().substring(0, 4))) return true;
        if (Stat.isLocalDefenseStat(line)) {
            String lineLower = line.toLowerCase().trim();
            for (Map.Entry<String, ItemDefenseSetter> entry : defenseSetterMap.entrySet()) {
                if (lineLower.startsWith(entry.getKey())) {
                    Matcher defMat = Pattern.compile("\\d+").matcher(line);
                    if (defMat.find()) entry.getValue().setDefense(item, Integer.parseInt(defMat.group()));
                    break;
                }
            }
            return true;
        }
        return false;
    }

    private static ParsedStat parseStat(String rawLine, boolean useTrueValues) {
        String line = rawLine;
        String addsPrefix = line.toLowerCase().startsWith("adds ") ? "Adds " : "";
        if (!addsPrefix.isEmpty()) {
            line = line.substring("adds ".length()).trim();
        }

        String rangeDisplay = null;
        Pattern flatDmgPattern = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)\\s*to\\s*([+-]?\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        Matcher flatMatcher = flatDmgPattern.matcher(line);
        if (flatMatcher.find()) {
            String before = line.substring(0, flatMatcher.start()).trim();
            String after = line.substring(flatMatcher.end()).trim();
            rangeDisplay = addsPrefix + (before.isEmpty() ? "" : before + " ") + flatMatcher.group(1) + "–" + flatMatcher.group(2) + " " + after;
            double n1 = Double.parseDouble(flatMatcher.group(1));
            double n2 = Double.parseDouble(flatMatcher.group(2));
            line = Math.round((n1 + n2) / 2.0) + line.substring(flatMatcher.end()).trim();
        }

        Pattern pattern = Pattern.compile("[+-]?\\d+(\\.\\d+)?%?\\s?");
        Matcher matcher = pattern.matcher(line);
        String stat = line;
        String displayText = line;
        String searchKey = line.toLowerCase().trim();
        double numericRoll = -1;
        if (matcher.find()) {
            String matched = matcher.group();
            String prefix = line.substring(0, matcher.start()).trim();
            String suffix = line.substring(matcher.end()).trim();
            boolean hasPercent = matched.contains("%");
            boolean hasSign = matched.charAt(0) == '+' || matched.charAt(0) == '-';
            double rawRoll = Double.parseDouble(matched.replaceAll("[^0-9.]", ""));
            stat = suffix;
            searchKey = ((prefix.isEmpty() ? "" : prefix + " ")
                    + (hasSign ? matched.charAt(0) : "") + "#" + (hasPercent ? "%" : "")
                    + (suffix.isEmpty() ? "" : " " + suffix)).toLowerCase().trim();
            String placeholder = (hasSign ? String.valueOf(matched.charAt(0)) : "") + "#" + (hasPercent ? "%" : "");
            displayText = addsPrefix + (prefix.isEmpty() ? "" : prefix + " ") + placeholder + (suffix.isEmpty() ? "" : " " + suffix);
            boolean excluded = PropertiesManagerCore.getRollDeltaExclusions().stream().anyMatch(searchKey::contains);
            numericRoll = (excluded || useTrueValues) ? (int) rawRoll : (int)(rawRoll * PropertiesManagerCore.getStatRollDelta());
        }
        if (rangeDisplay != null) {
            displayText = rangeDisplay;
        }
        return new ParsedStat(stat, displayText, searchKey, numericRoll);
    }

    private static Stat resolveImplicitStat(ParsedStat ps, Build build) {
        String implicitId = build.getIdFromStatText(ps.searchKey(), "implicit");
        String enchantId  = build.getIdFromStatText(ps.searchKey(), "enchant");
        List<String> ids = new ArrayList<>();
        if (implicitId != null) ids.add(implicitId);
        if (enchantId != null && !ids.contains(enchantId)) ids.add(enchantId);
        String primaryId = ids.isEmpty() ? build.getIdFromStatText(ps.searchKey()) : ids.get(0);
        Stat newStat = new Stat(ps.displayText(), primaryId, ps.numericRoll());
        if (ids.size() >= 2) newStat.setAlternateId(ids.get(1));
        return newStat;
    }

    private static StatResolution resolveStatId(String searchKey, double numericRoll, int lineIdx, String[] lines, Set<Integer> skipLines, Build build) {
        if (lineIdx + 2 < lines.length && !skipLines.contains(lineIdx + 1) && !skipLines.contains(lineIdx + 2)) {
            String threeLineKey = searchKey + "\n" + normalizeLineToSearchKey(lines[lineIdx + 1], build.isUseTrueValues()) + "\n" + normalizeLineToSearchKey(lines[lineIdx + 2], build.isUseTrueValues());
            String threeLineId = build.getIdFromStatText(threeLineKey);
            if (threeLineId != null) { skipLines.add(lineIdx + 1); skipLines.add(lineIdx + 2); return new StatResolution(threeLineId, numericRoll); }
        }
        if (lineIdx + 1 < lines.length && !skipLines.contains(lineIdx + 1)) {
            String twoLineKey = searchKey + "\n" + normalizeLineToSearchKey(lines[lineIdx + 1], build.isUseTrueValues());
            String twoLineId = build.getIdFromStatText(twoLineKey);
            if (twoLineId != null) { skipLines.add(lineIdx + 1); return new StatResolution(twoLineId, numericRoll); }
        }
        String id = build.getIdFromStatText(searchKey);
        if (id != null) return new StatResolution(id, numericRoll);
        if (searchKey.contains("reduced")) {
            String increasedKey = searchKey.replaceFirst("reduced", "increased");
            String increasedId = build.getIdFromStatText(increasedKey);
            if (increasedId != null) return new StatResolution(increasedId, -numericRoll);
        }
        return new StatResolution(null, numericRoll);
    }

    private static List<Stat> collapseStats(List<Stat> stats) {
        Map<String, Stat> byId = new LinkedHashMap<>();
        List<Stat> unmatched = new ArrayList<>();
        for (Stat s : stats) {
            if (s.getId() == null) { unmatched.add(s); continue; }
            byId.merge(s.getId(), s, (existing, next) ->
                    new Stat(existing.getText(), existing.getId(), existing.getRoll() + next.getRoll()));
        }
        List<Stat> collapsed = new ArrayList<>(byId.values());
        collapsed.addAll(unmatched);
        return collapsed;
    }

    private static void computeAndApplyWeaponDps(Item item, String baseType, Build build, Map<String, Double> weaponDmg) {
        WeaponBaseStatsProperties baseStatsProps = build.getWeaponBaseStats();
        if (baseStatsProps == null) return;
        WeaponBaseStats baseStats = baseStatsProps.getStats(baseType);
        if (baseStats == null) return;
        double physFlatDmg = weaponDmg.getOrDefault("physical damage",           0.0);
        double physIncrPct = weaponDmg.getOrDefault("increased physical damage", 0.0);
        double apsIncrPct  = weaponDmg.getOrDefault("increased attack speed",    0.0);
        double fireDmg     = weaponDmg.getOrDefault("fire damage",               0.0);
        double coldDmg     = weaponDmg.getOrDefault("cold damage",               0.0);
        double lightDmg    = weaponDmg.getOrDefault("lightning damage",          0.0);
        double finalAps    = baseStats.aps * (1.0 + apsIncrPct / 100.0);
        double basePhysAvg = (baseStats.physMin + baseStats.physMax) / 2.0;
        double totalPhys   = (basePhysAvg + physFlatDmg) * (1.0 + (item.quality + physIncrPct) / 100.0);
        item.pdps = (int) Math.floor(totalPhys * finalAps);
        item.edps = (int) Math.floor((fireDmg + coldDmg + lightDmg) * finalAps);
    }

    private static String normalizeLineToSearchKey(String rawLine, boolean useTrueValues) {
        return parseStat(rawLine, useTrueValues).searchKey();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(name))           builder.append(name);
        else if (StringUtils.isNotBlank(baseType))  builder.append(baseType);
        else if (StringUtils.isNotBlank(type))      builder.append(type);
        else                                         builder.append("Missing String");
        if (CollectionUtils.isNotEmpty(stats)) {
            builder.append(" ");
            for (Stat stat : stats) {
                builder.append(stat.getText()).append(" ");
                if (stat.getRoll() != -1) builder.append(stat.getRoll()).append(" ");
            }
        }
        return builder.toString();
    }
}
