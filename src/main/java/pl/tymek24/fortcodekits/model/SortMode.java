package pl.tymek24.fortcodekits.model;

public enum SortMode {
  DEFAULT("default", "DOMYSLNY"),
  NAME("name", "NAZWA"),
  COOLDOWN("cooldown", "COOLDOWN"),
  STATUS("status", "STATUS"),
  RARITY("rarity", "KLASA");

  private final String key;
  private final String label;

  SortMode(String key, String label) {
    this.key = key;
    this.label = label;
  }

  public String key() {
    return this.key;
  }

  public String label() {
    return this.label;
  }

  public SortMode next() {
    SortMode[] all = values();
    int index = ordinal() + 1;
    return index >= all.length ? all[0] : all[index];
  }

  public static SortMode fromKey(String raw) {
    if (raw == null) {
      return DEFAULT;
    }
    for (SortMode mode : values()) {
      if (mode.key.equalsIgnoreCase(raw.trim())) {
        return mode;
      }
    }
    return DEFAULT;
  }
}
