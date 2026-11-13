package pl.tymek24.fortcodekits.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
  private final ItemStack itemStack;

  public ItemBuilder(Material material) {
    this.itemStack = new ItemStack(material);
  }

  public ItemBuilder(ItemStack base) {
    this.itemStack = base.clone();
  }

  public ItemBuilder name(String name) {
    ItemMeta meta = this.itemStack.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(ColorUtil.colorize(name));
      this.itemStack.setItemMeta(meta);
    }
    return this;
  }

  public ItemBuilder lore(List<String> lore) {
    ItemMeta meta = this.itemStack.getItemMeta();
    if (meta != null) {
      meta.setLore(ColorUtil.colorize(new ArrayList<String>(lore)));
      this.itemStack.setItemMeta(meta);
    }
    return this;
  }

  public ItemBuilder enchant(Enchantment enchantment, int level) {
    this.itemStack.addUnsafeEnchantment(enchantment, level);
    return this;
  }

  public ItemBuilder hideFlags() {
    ItemMeta meta = this.itemStack.getItemMeta();
    if (meta != null) {
      meta.addItemFlags(ItemFlag.values());
      this.itemStack.setItemMeta(meta);
    }
    return this;
  }

  public ItemBuilder enchantments(Map<Enchantment, Integer> map) {
    map.forEach((arg_0, arg_1) -> ((ItemStack) this.itemStack).addUnsafeEnchantment(arg_0, arg_1));
    return this;
  }

  public ItemStack build() {
    return this.itemStack.clone();
  }
}
