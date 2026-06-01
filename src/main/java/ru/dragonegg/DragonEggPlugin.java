
public final class DragonEggPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Boolean> activePlayers = new HashMap<>();
    private final AttributeModifier healthModifier = new AttributeModifier(
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
            "dragon_egg_health",
            4.0,
            AttributeModifier.Operation.ADD_NUMBER
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        startEffectTask();
        getLogger().info("DragonEggPlugin включён!");
    }

    @Override
    public void onDisable() {
        for (UUID uuid : activePlayers.keySet()) {
            Player player = getServer().getPlayer(uuid);
            if (player != null) {
                removeEffects(player);
            }
        }
        activePlayers.clear();
        getLogger().info("DragonEggPlugin выключен!");
    }

    private void startEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    boolean hasEgg = hasDragonEgg(player.getInventory());
                    if (hasEgg) {
                        if (!activePlayers.containsKey(player.getUniqueId()) || !activePlayers.get(player.getUniqueId())) {
                            applyEffects(player);
                        }
                        refreshEffects(player);
                        activePlayers.put(player.getUniqueId(), true);
                    } else {
                        if (activePlayers.containsKey(player.getUniqueId()) && activePlayers.get(player.getUniqueId())) {
                            removeEffects(player);
                        }
                        activePlayers.put(player.getUniqueId(), false);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private boolean hasDragonEgg(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        ItemStack offHand = inventory.getItemInOffHand();
        return offHand.getType() == Material.DRAGON_EGG;
    }

    private void applyEffects(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null && !maxHealth.getModifiers().contains(healthModifier)) {
            maxHealth.addModifier(healthModifier);
            player.setHealth(Math.min(player.getHealth() + 4.0, maxHealth.getValue()));
        }
    }

    private void refreshEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1, true, true));
    }

    private void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifiers().contains(healthModifier)) {
            maxHealth.removeModifier(healthModifier);
            if (player.getHealth() > maxHealth.getValue()) {
                player.setHealth(maxHealth.getValue());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;
        if (clickedInventory.getType() == InventoryType.ENDER_CHEST) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            if (cursor != null && cursor.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                return;
            }
            if (event.isShiftClick() && current != null && current.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getView().getTopInventory().getType() == InventoryType.ENDER_CHEST) {
            if (event.isShiftClick() && event.getCurrentItem() != null
                    && event.getCurrentItem().getType() == Material.DRAGON_EGG) {
                if (clickedInventory.getType() == InventoryType.ENDER_CHEST) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasDragonEgg(player.getInventory())) {
            applyEffects(player);
            activePlayers.put(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeEffects(player);
        activePlayers.remove(player.getUniqueId());
    }
}
