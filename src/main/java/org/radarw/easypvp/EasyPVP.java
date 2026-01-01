package org.radarw.easypvp;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static java.lang.Integer.parseInt;

public final class EasyPVP extends JavaPlugin implements Listener {
    public ScoreboardManager manager; //
    public Scoreboard board; //
    public Team team; //
    private static EasyPVP instance;
    public int saveinterval = 5; // mins

    Gson gson = new Gson();

    public Map<String, PlayerData> dataMap = new HashMap<>();
    public Map<String, PlayerData> dataMap_snapshot = new HashMap<>();
    File dataFile = new File(getDataFolder(), "data.json");

    File toolsFile = new File(getDataFolder(), "tools.json");
    File enchantsFile = new File(getDataFolder(), "enchants.json");

    public Map<String, Integer> item_values = new HashMap<>();
    public Map<String, List<Map<String, Integer>>> enchant_values = new HashMap<>();

    public void loadItemValues() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(toolsFile)) {
            item_values = gson.fromJson(
                    reader,
                    new TypeToken<Map<String, Integer>>(){}.getType()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //save resourec when u have a chance xxxxx

    public void loadEnchantValues() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(enchantsFile)) {
            enchant_values = gson.fromJson(
                    reader,
                    new TypeToken<Map<String, List<Map<String, Integer>>>>(){}.getType()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean areanstate = true; // true = open, false = closed

    public void saveData() { // hash map --> file
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(dataMap, writer);
            Bukkit.getServer().getConsoleSender().sendMessage("SAVED DATA MAP.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() { // file --> hash map
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, PlayerData>>() {}.getType();
            dataMap = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void spawnMoneyItem(Location Loc, int amount){
        World world = Loc.getWorld();
        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldIngotMetadata = goldIngot.getItemMeta();

        goldIngotMetadata.setDisplayName(ChatColor.YELLOW + "Gold");
        goldIngotMetadata.addEnchant(Enchantment.EFFICIENCY, 10, true);

        goldIngot.setItemMeta(goldIngotMetadata);

        for (int i = 0; i < amount; i++) {
            world.dropItem(Loc, goldIngot);
        }
    }

    public void giveMoneyItem(Player p, int amount){
        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldIngotMetadata = goldIngot.getItemMeta();

        goldIngotMetadata.setDisplayName(ChatColor.YELLOW + "Gold");
        goldIngotMetadata.addEnchant(Enchantment.EFFICIENCY, 10, true);

        goldIngot.setItemMeta(goldIngotMetadata);

        for (int i = 0; i < amount; i++) {
            p.getInventory().addItem(goldIngot);
        }
    }

    public boolean canItemFit(Player p, int amount, int stackSize) {
        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT); // was from preexisting code, this DOESNT give VALID money.
        ItemMeta meta = goldIngot.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "THIS SHOULD BE HERE");
        meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
        goldIngot.setItemMeta(meta);

        goldIngot.setAmount(stackSize);

        int remaining = amount;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack == null) {
                remaining -= stackSize;
            } else if (stack.isSimilar(goldIngot)) {
                remaining -= (stackSize - stack.getAmount());
            }

            if (remaining <= 0) return true;
        }
        return false;
    }

    public static int getItemAmount(Player player) {
        int total = 0;

        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldIngotMetadata = goldIngot.getItemMeta();

        goldIngotMetadata.setDisplayName(ChatColor.YELLOW + "Gold");
        goldIngotMetadata.addEnchant(Enchantment.EFFICIENCY, 10, true);

        goldIngot.setItemMeta(goldIngotMetadata);

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) continue;

            // checks type + meta (name, lore, enchants, etc.)
            if (item.isSimilar(goldIngot)) {
                total += item.getAmount();
            }
        }

        return total;
    }

    public static boolean removeItemAmount(Player player, int amount) {

        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldIngotMetadata = goldIngot.getItemMeta();

        goldIngotMetadata.setDisplayName(ChatColor.YELLOW + "Gold");
        goldIngotMetadata.addEnchant(Enchantment.EFFICIENCY, 10, true);

        goldIngot.setItemMeta(goldIngotMetadata);

        int remaining = amount;
        PlayerInventory inv = player.getInventory();

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);

            if (item == null) continue;
            if (!item.isSimilar(goldIngot)) continue;

            int stackAmount = item.getAmount();

            if (stackAmount <= remaining) {
                // remove whole stack
                inv.setItem(slot, null);
                remaining -= stackAmount;
            } else {
                // remove part of stack
                item.setAmount(stackAmount - remaining);
                remaining = 0;
            }

            if (remaining <= 0) break;
        }

        return remaining == 0; // true if fully removed
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "data.json");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();

                PlayerData data = null; // wouldn't exist as this is a new file
                UUID fakePlayer = UUID.fromString("738eaf75-2a10-4756-887f-30f76e4ee744");
                data = new PlayerData(fakePlayer, 0, 0, 0);
                dataMap.put(String.valueOf(fakePlayer), data);

                saveData();

                Bukkit.getConsoleSender().sendMessage("CREATED DATAMAP & DATAFILE!");
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop"); // if this fails, we are screwed. better to stop the server inorder to let admins know
                // there is a dangerous problem with their config.
            }
        } else {
            Bukkit.getConsoleSender().sendMessage("DATAMAP FOUND!");
            loadData();
            if (dataMap.get("738eaf75-2a10-4756-887f-30f76e4ee744") != null){ // if 738eaf75-2a10-4756-887f-30f76e4ee744 is still in the code:
                dataMap_snapshot = dataMap; // copy datamap, we don't want to work on live data.

                // this uuid is a banned account, we just need it for this purpose. we dont really care abt this data as it will never be used.
                // TODO: improve this as this is a very hacky soultion, in a ideal world this shouldnt be a thing.

                dataMap_snapshot.remove("738eaf75-2a10-4756-887f-30f76e4ee744"); // !!SNAPSHOT!!, DO NOT WORK ON "dataMap".

                if (dataMap_snapshot != null){
                    Bukkit.getConsoleSender().sendMessage("Real Data created! Removing empty Player...");
                    dataMap.remove("738eaf75-2a10-4756-887f-30f76e4ee744");
                }
                saveData();

                dataMap_snapshot = null; // remove data leakage
            }
            loadEnchantValues();
            loadItemValues();
        }

        manager = Bukkit.getScoreboardManager();

        getServer().getPluginManager().registerEvents(this, this);
        instance = this;

        new BukkitRunnable() {
            @Override
            public void run(){
                saveData();
            }
        }.runTaskTimer(getInstance(), 20L, 20L*60*saveinterval);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule keepInventory false");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    public static boolean getRegions(Player player, String regionName) {
        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) return false;

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return false;

        BlockVector3 point = BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        );

        return region.contains(point);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // NOTICE: This command is backend FOR the /shop feature. This has been exposed for ease of use, and mods can interact with this
        // to give players features such as quick buying gear and loot.

        if (cmd.getName().equalsIgnoreCase("buy")){
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c[!] Only players can use this command");
                return false;
            }

            if (!getRegions(((Player) sender).getPlayer(), "spawn")){
                sender.sendMessage("§c[!] You have to be in spawn to execute this command!");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage("§c[!] Invalid Item");
                return false;
            }

            int toolValue = item_values.getOrDefault("minecraft:"+args[0].toLowerCase(), 0);
            String uuid = ((Player) sender).getPlayer().getUniqueId().toString();
            PlayerData pd = dataMap.get(uuid);

            int amount = 1; // default

            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    amount = 1;
                }
            }

            if (toolValue != 0){
                if (pd.money >= (toolValue * amount)){
                    Material mat;
                    mat = Material.valueOf(args[0].toUpperCase());
                    ItemStack item = new ItemStack(mat);

                    if (canItemFit(((Player) sender).getPlayer(), amount, item.getMaxStackSize())){
                        pd.money -= (toolValue*amount);

                        for (int i = 0; i < amount; i++) {
                            ((Player) sender).getInventory().addItem(item);
                        }

                        update_Score_board(pd.kills, pd.deaths, pd.money, ((Player) sender).getPlayer());

                        if (amount > 1){
                            sender.sendMessage("§a[!] Successfully brought " + amount + " " +args[0].substring(0, 1).toUpperCase() + args[0].substring(1));
                        }else{
                            sender.sendMessage("§a[!] Successfully brought " + args[0].substring(0, 1).toUpperCase() + args[0].substring(1));
                        }

                    }else {
                        sender.sendMessage("§c[!] Cannot fit Items!");
                    }

                }else{
                    sender.sendMessage("§c[!] You dont have enough gold!");
                }

            }else{
                sender.sendMessage("§c[!] Invalid Item");
            }
        }

        if (cmd.getName().equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c[!] Only players can use this command");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage("§c[!] Invalid Gold amount");
                return false;
            }

            if (!getRegions(((Player) sender).getPlayer(), "spawn")){
                sender.sendMessage("§c[!] You have to be in spawn to execute this command!");
                return false;
            }

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            // example usage
            PlayerData pd = dataMap.get(uuid.toString());
            int amount = Integer.parseInt(args[0]);

            if (pd.money >= amount){
                if (canItemFit(player, amount, 64)){ // hardcoded, gold can only be in stacks of 64.
                    pd.money -= amount;
                    giveMoneyItem(player, amount);
                }else{
                    sender.sendMessage("§c[!] Amount will not fit in inventory!");
                    return false;
                }
            }else{
                sender.sendMessage("§c[!] Not Enough gold!");
                return false;
            }
            update_Score_board(pd.kills, pd.deaths, pd.money, player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("givegold")) { // admin
            if (!sender.isOp()) {
                sender.sendMessage("§cNo Permissions");
                return false;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return false;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount!");
                return false;
            }

            giveMoneyItem(target, amount);
            sender.sendMessage("§aSuccessfully gave " + amount + " gold to " + target.getName() + "!");
        }

        if (cmd.getName().equalsIgnoreCase("sellitems")) {
            if (!getRegions(((Player) sender).getPlayer(), "spawn")){
                sender.sendMessage("§c[!] You have to be in spawn to execute this command!");
                return false;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("§c[!] Only players can use this command");
                return false;
            }

            final Inventory inv;

            inv = Bukkit.createInventory(null, 9, sender.getName());
            Player player = (Player) sender;
            player.openInventory(inv);
        }

        if (cmd.getName().equalsIgnoreCase("deposit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c[!] Only players can use this command");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage("§c[!] Invalid Gold amount");
                return false;
            }

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            // example usage
            PlayerData pd = dataMap.get(uuid.toString());
            int amount = Integer.parseInt(args[0]);
            int playerAmount = getItemAmount(player); // true val

            if (playerAmount < amount) {
                removeItemAmount(player, playerAmount);
                pd.money += playerAmount;
                sender.sendMessage("§a[!] Deposited " + playerAmount + " gold!");
            } else {
                removeItemAmount(player, amount);
                pd.money += amount;
                sender.sendMessage("§a[!] Deposited " + amount + " gold!");
            }

            update_Score_board(pd.kills, pd.deaths, pd.money, player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("amialive")) {
            sender.sendMessage("Hey there! You ran the /hello command.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("shop")){
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c[!] Only players can use this command");
                return false;
            }
            sender.sendMessage("§c[!] we are working o this fuck off");

            // wrapper for buy command???
        }

        if (cmd.getName().equalsIgnoreCase("kpdebug")){ // secret command
            if (!sender.isOp()) {
                sender.sendMessage("§cNo Permissions");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage("§cDEVS ONLY, CONTACT SOMEONE IF YOU NEED ACCESS, IF NOT, FUCK OFF.");
                return false;
            }

            if (args[0].equalsIgnoreCase("savedata")) {
                if (args[1].equalsIgnoreCase("CONFIRM")) { // check
                    saveData();
                    sender.sendMessage("§csaved");
                    return true;
                }
            }

            if (args[0].equalsIgnoreCase("loaddata")) {
                if (args[1].equalsIgnoreCase("CONFIRM")) { // check
                    loadData();
                    sender.sendMessage("§cloaded");
                    return true;
                }
            }
        }

        if (cmd.getName().equalsIgnoreCase("arena")) { // removed barrier blocks
            if (!sender.isOp()) {
                sender.sendMessage("§cNo Permissions");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage("§cUsage: /arena <open|close>");
                return true;
            }

            if (args[0].equalsIgnoreCase("close")) {
                areanstate = false;
                Bukkit.broadcastMessage("§cClosed PVP Arena");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (getRegions(p, "pvparea") && !getRegions(p, "spawn") && p.getGameMode() == GameMode.SURVIVAL) {
                        p.teleport(new Location(p.getWorld(), 15, -55, 5));
                        p.sendMessage("§cYou have been teleported due to the arena closing..");
                    }
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule keepInventory true");

                new BukkitRunnable() {
                    @Override
                    public void run(){
                        if (areanstate == false){
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (getRegions(p, "pvparea") && !getRegions(p, "spawn") && p.getGameMode() == GameMode.SURVIVAL) {
                                    p.teleport(new Location(p.getWorld(), 15, -55, 5));
                                    p.sendMessage("§cYou have been teleported due to the arena closing..");
                                }
                            }
                        }else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule keepInventory false");
                            cancel();
                        }
                    }
                }.runTaskTimer(getInstance(), 20L, 20L*1);

            } else if (args[0].equalsIgnoreCase("open")) {
                Bukkit.broadcastMessage("§aOpened PVP Arena");
                areanstate = true;
            } else {
                sender.sendMessage("§cUnknown subcommand! Use /arena <open|close>");
            }
            return true;
        }
        return false;
    }

    public void update_Score_board(int kills, int deaths, int money, Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective(
                "stats",
                "dummy",
                ChatColor.YELLOW + "Player Stats"
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team killsTeam = board.registerNewTeam("kills");
        Team deathsTeam = board.registerNewTeam("deaths");
        Team kdrTeam = board.registerNewTeam("kdr");
        Team moneyTeam = board.registerNewTeam("money");

        String killsEntry = ChatColor.GREEN + "Kills:";
        String deathsEntry = ChatColor.GREEN + "Deaths:";
        String kdrEntry = ChatColor.GREEN + "KDR:";
        String moneyEntry = ChatColor.GREEN + "Money:";

        killsTeam.addEntry(killsEntry);
        deathsTeam.addEntry(deathsEntry);
        kdrTeam.addEntry(kdrEntry);
        moneyTeam.addEntry(moneyEntry);

        float kdRatioNum = deaths == 0 ? kills : (float) kills / deaths;

        killsTeam.setSuffix(ChatColor.WHITE + " " + kills);
        deathsTeam.setSuffix(ChatColor.WHITE + " " + deaths);
        kdrTeam.setSuffix(ChatColor.WHITE + " " + String.format("%.3f", kdRatioNum));
        moneyTeam.setSuffix(ChatColor.WHITE + " " + money);

        obj.getScore(killsEntry).setScore(3);   // higher = top
        obj.getScore(deathsEntry).setScore(2);
        obj.getScore(kdrEntry).setScore(1);
        obj.getScore(moneyEntry).setScore(0);

        player.setScoreboard(board);
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = event.getEntity().getKiller();

        event.setDeathMessage(null);

        if (areanstate) {
            if (killer != null){
                killer.sendMessage("[!] You killed " + victim.getName() + "!");
                victim.sendMessage("[!] You were slain by " + killer.getName() + "!");

                killer.setHealth(Math.min(
                        killer.getHealth() + 6.0F,
                        killer.getAttribute(Attribute.MAX_HEALTH).getValue()
                ));

                PlayerData kd = dataMap.get(killer.getUniqueId().toString());
                PlayerData vd = dataMap.get(victim.getUniqueId().toString());

                kd.kills++;

                vd.deaths++;

                update_Score_board(kd.kills, kd.deaths, kd.money, killer);
                update_Score_board(vd.kills, vd.deaths, vd.money, victim);
            }else{
                victim.sendMessage("[!] You Died!");
                PlayerData vd = dataMap.get(victim.getUniqueId().toString());
                vd.deaths++;
                update_Score_board(vd.kills, vd.deaths, vd.money, victim);
            }
            Bukkit.getScheduler().runTaskLater(instance, victim.spigot()::respawn, 1);
            spawnMoneyItem(victim.getLocation(), 5);
        } else {
            killer.sendMessage("[!] Arena Closed: Stats or Inventory not dropped.");
            victim.sendMessage("[!] Arena Closed: Stats or Inventory not dropped.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String uuid = p.getUniqueId().toString();

        PlayerData data = dataMap.get(uuid);
        if (data == null && dataMap != null) {
            data = new PlayerData(p.getUniqueId(), 0, 0, 0);
            dataMap.put(uuid, data);
            p.sendMessage("Welcome!");
        } else {
            p.sendMessage("Welcome back!");
        }

        if (dataMap == null){
            p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");p.sendMessage("DATAMAP IS EMPTY, CONTACT SOMEONE ABOUT THIS!");
        }

        update_Score_board(data.kills, data.deaths, data.money, p);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryView view = e.getView();
        if (view.getTitle().equals(e.getPlayer().getName())) { // sell gui logic
            Inventory inv = e.getInventory();
            ItemStack[] items = inv.getContents();
            int TotalVal = 0;

            for (int i = 0; i < items.length; i++){ // loop through items
                ItemStack item = items[i];
                if (item == null){
                    continue;
                }
                // if item is in tools.json
                ItemMeta meta = item.getItemMeta();
                int enchant_value = 0;
                int toolValue = 0;
                if (item != null && item.getType() != Material.AIR){
                    String key = "minecraft:" + item.getType().toString().toLowerCase();
                    toolValue = item_values.getOrDefault(key, 0);
                    if (toolValue == 0){
                        e.getPlayer().getInventory().addItem(item);
                        continue;
                    }

                    if (toolValue > 0 && meta != null){
                        Enchantment[] enchants = meta.getEnchants().keySet().toArray(new Enchantment[0]);

                        for (Enchantment Enchant : enchants){
                            int level = meta.getEnchantLevel(Enchant);
                            String enchantKey = Enchant.getKey().toString();
                            List<Map<String, Integer>> levels = enchant_values.get(enchantKey);
                            // default if not found
                            if (levels != null) { // stopping here??????
                                for (Map<String, Integer> map : levels) {
                                    if (map.get("level").equals(level)) {
                                        enchant_value = map.get("value");
                                        break; // stop once found
                                    }
                                }
                            }
                        }
                    }
                }
                int amount = item.getAmount();
                TotalVal += ((enchant_value + toolValue) * amount) * (1-0.25); // times by num of items in the stack, per item slot. - take 25% off the price of the items.
                // stupid way of doing this. but icba rn TODO: CHANGE THIS, ADD ANOTHER ITEM SCHEMA.
            }
            String uuid = e.getPlayer().getUniqueId().toString();
            PlayerData pd = dataMap.get(uuid);
            e.getPlayer().sendMessage("[!] Sold for " + TotalVal);
            pd.money += TotalVal;
            HumanEntity player = e.getPlayer();
            update_Score_board(pd.kills, pd.deaths, pd.money, (Player) player);
        }
    }



    public static Plugin getInstance() {
        return instance;
    }
}
