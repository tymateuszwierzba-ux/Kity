package pl.tymek24.fortcodekits.gui;

import java.util.Collections;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {
  private final GuiType type;
  private final Map<Integer, String> kitSlots;
  private final Map<Integer, EditorAction> editorActions;
  private final String kitId;
  private final int page;
  private final GuiType returnType;
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

  public void setInventory(Inventory inventory) {
    this.inventory = inventory;
  }

  public Inventory getInventory() {
    return this.inventory;
  }
}
