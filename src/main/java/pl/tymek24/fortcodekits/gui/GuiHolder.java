package pl.tymek24.fortcodekits.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pl.tymek24.fortcodekits.model.SortMode;

public class GuiHolder implements InventoryHolder {
  private final GuiType type;
  private final Map<Integer, String> kitSlots;
  private final Map<Integer, EditorAction> editorActions;
  private final String kitId;
  private final int page;
  private final GuiType returnType;
  private String confirmAction;
  private String categoryFilter;
  private String searchQuery;
  private SortMode sortMode;
  private Inventory inventory;

  public GuiHolder(
      GuiType type,
      Map<Integer, String> kitSlots,
      Map<Integer, EditorAction> editorActions,
      String kitId,
      int page,
      GuiType returnType) {
    this.type = type;
    this.kitSlots = kitSlots == null ? Collections.emptyMap() : kitSlots;
    this.editorActions = editorActions == null ? Collections.emptyMap() : editorActions;
    this.kitId = kitId;
    this.page = page;
    this.returnType = returnType;
    this.confirmAction = null;
    this.categoryFilter = null;
    this.searchQuery = null;
    this.sortMode = SortMode.DEFAULT;
  }

  public GuiType getType() {
    return this.type;
  }

  public Map<Integer, String> getKitSlots() {
    return this.kitSlots;
  }

  public Map<Integer, EditorAction> getEditorActions() {
    return this.editorActions;
  }

  public String getKitId() {
    return this.kitId;
  }

  public int getPage() {
    return this.page;
  }

  public GuiType getReturnType() {
    return this.returnType;
  }

  public String getConfirmAction() {
    return this.confirmAction;
  }

  public void setConfirmAction(String confirmAction) {
    this.confirmAction = confirmAction;
  }

  public String getCategoryFilter() {
    return this.categoryFilter;
  }

  public void setCategoryFilter(String categoryFilter) {
    this.categoryFilter = categoryFilter;
  }

  public String getSearchQuery() {
    return this.searchQuery;
  }

  public void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  public SortMode getSortMode() {
    return this.sortMode == null ? SortMode.DEFAULT : this.sortMode;
  }

  public void setSortMode(SortMode sortMode) {
    this.sortMode = sortMode == null ? SortMode.DEFAULT : sortMode;
  }

  public Map<String, Object> snapshot() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("type", this.type.name());
    map.put("page", this.page);
    map.put("kitId", this.kitId);
    map.put("returnType", this.returnType == null ? null : this.returnType.name());
    map.put("confirmAction", this.confirmAction);
    map.put("category", this.categoryFilter);
    map.put("search", this.searchQuery);
    map.put("sort", this.getSortMode().key());
    return map;
  }

  public void setInventory(Inventory inventory) {
    this.inventory = inventory;
  }

  public Inventory getInventory() {
    return this.inventory;
  }
}
