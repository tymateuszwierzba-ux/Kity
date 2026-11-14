package pl.tymek24.fortcodekits.integration;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class EconomyHook {
  private static Object economy;
  private static Method getBalance;
  private static Method withdraw;
  private static Method deposit;
  private static Method hasAccount;
  private static Method format;
  private static boolean ready;

  private EconomyHook() {}

  public static void init() {
    ready = false;
    economy = null;
    if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
      return;
    }
    try {
      Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
      Object registration =
          Bukkit.getServicesManager().getRegistration((Class) economyClass);
      if (registration == null) {
        return;
      }
      Method getProvider = registration.getClass().getMethod("getProvider");
      economy = getProvider.invoke(registration);
      if (economy == null) {
        return;
      }
      getBalance = economyClass.getMethod("getBalance", Player.class);
      withdraw = economyClass.getMethod("withdrawPlayer", Player.class, double.class);
      deposit = economyClass.getMethod("depositPlayer", Player.class, double.class);
      hasAccount = economyClass.getMethod("hasAccount", Player.class);
      try {
        format = economyClass.getMethod("format", double.class);
      } catch (NoSuchMethodException ex) {
        format = null;
      }
      ready = true;
    } catch (Throwable ex) {
      ready = false;
      economy = null;
    }
  }

  public static boolean isReady() {
    return ready && economy != null;
  }

  public static double getBalance(Player player) {
    if (!isReady() || player == null) {
      return 0.0D;
    }
    try {
      Object result = getBalance.invoke(economy, player);
      return result instanceof Double value ? value : 0.0D;
    } catch (Throwable ex) {
      return 0.0D;
    }
  }

  public static boolean has(Player player, double amount) {
    if (!isReady() || player == null) {
      return amount <= 0.0D;
    }
    try {
      hasAccount.invoke(economy, player);
      return getBalance(player) >= amount;
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean withdraw(Player player, double amount) {
    if (!isReady() || player == null || amount <= 0.0D) {
      return true;
    }
    try {
      Object response = withdraw.invoke(economy, player, amount);
      if (response == null) {
        return false;
      }
      Method success = response.getClass().getMethod("transactionSuccess");
      return Boolean.TRUE.equals(success.invoke(response));
    } catch (Throwable ex) {
      return false;
    }
  }

  public static boolean deposit(Player player, double amount) {
    if (!isReady() || player == null || amount <= 0.0D) {
      return true;
    }
    try {
      Object response = deposit.invoke(economy, player, amount);
      if (response == null) {
        return false;
      }
      Method success = response.getClass().getMethod("transactionSuccess");
      return Boolean.TRUE.equals(success.invoke(response));
    } catch (Throwable ex) {
      return false;
    }
  }

  public static String format(double amount) {
    if (isReady() && format != null) {
      try {
        Object result = format.invoke(economy, amount);
        if (result != null) {
          return String.valueOf(result);
        }
      } catch (Throwable ignored) {
      }
    }
    return String.format("%.2f", amount);
  }

  public static String currencySymbol() {
    if (!isReady()) {
      return "MONETY";
    }
    try {
      Method method = economy.getClass().getMethod("currencyNamePlural");
      Object result = method.invoke(economy);
      if (result != null) {
        return String.valueOf(result);
      }
    } catch (Throwable ignored) {
    }
    return "MONETY";
  }

  public static UUID ownerUuid() {
    return null;
  }
}
