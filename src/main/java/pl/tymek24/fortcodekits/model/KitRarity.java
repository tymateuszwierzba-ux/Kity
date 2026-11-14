package pl.tymek24.fortcodekits.model;

import java.util.Locale;

public enum KitRarity {
  COMMON("common", "&7"),
  UNCOMMON("uncommon", "&a"),
  RARE("rare", "&b"),
  EPIC("epic", "&d"),
  LEGENDARY("legendary", "&6");

  private final String key;
  private final String color;

  KitRarity(String key, String color) {
    this.key = key;
    this.color = color;
  }

  public String key() {
    return this.key;
  }

  public String color() {
    return this.color;
  }

  public String display() {
    return this.color + this.name();
  }

  public static KitRarity from(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    if (value.isEmpty() || value.equals("default") || value.equals("none")) {
      return null;
    }
    for (KitRarity rarity : values()) {
      if (rarity.key.equals(value) || rarity.name().toLowerCase(Locale.ROOT).equals(value)) {
        return rarity;
      }
    }
    return null;
  }

  public static KitRarity next(KitRarity current) {
    if (current == null) {
      return COMMON;
    }
    KitRarity[] all = values();
    int index = current.ordinal() + 1;
    return index >= all.length ? null : all[index];
  }
}
