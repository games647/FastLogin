package com.github.games647.fastlogin.bukkit.hooks;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Achievement;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import ultraauth.api.UltraAuthAPI;

/**
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/ultraauth-aa/
 * Spigot: https://www.spigotmc.org/resources/ultraauth.17044/
 */
public class UltraAuthHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        UltraAuthAPI.authenticatedPlayer(player);
    }

    @Override
    public boolean isRegistered(String playerName) {
        return UltraAuthAPI.isRegisterd(new FakePlayer(playerName));
    }

    @Override
    public void forceRegister(Player player, String password) {
        UltraAuthAPI.setPlayerPasswordOnline(player, password);
    }

    class FakePlayer implements Player {

        private final String username;

        public FakePlayer(String username) {
            this.username = username;
        }

        @Override
        public String getDisplayName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDisplayName(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPlayerListName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPlayerListName(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCompassTarget(Location loc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Location getCompassTarget() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InetSocketAddress getAddress() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendRawMessage(String message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void kickPlayer(String message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void chat(String msg) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean performCommand(String command) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSneaking() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSneaking(boolean sneak) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSprinting() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSprinting(boolean sprinting) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void saveData() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void loadData() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSleepingIgnored(boolean isSleeping) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSleepingIgnored() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playNote(Location loc, byte instrument, byte note) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playNote(Location loc, Instrument instrument, Note note) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playSound(Location location, Sound sound, float volume, float pitch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playSound(Location location, String sound, float volume, float pitch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playEffect(Location loc, Effect effect, int data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void playEffect(Location loc, Effect effect, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendBlockChange(Location loc, Material material, byte data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendBlockChange(Location loc, int material, byte data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendSignChange(Location loc, String[] lines) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendMap(MapView map) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void updateInventory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void awardAchievement(Achievement achievement) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeAchievement(Achievement achievement) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasAchievement(Achievement achievement) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getStatistic(Statistic statistic) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPlayerTime(long time, boolean relative) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getPlayerTime() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getPlayerTimeOffset() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isPlayerTimeRelative() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void resetPlayerTime() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPlayerWeather(WeatherType type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public WeatherType getPlayerWeather() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void resetPlayerWeather() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void giveExp(int amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void giveExpLevels(int amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getExp() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setExp(float exp) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getLevel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setLevel(int level) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getTotalExperience() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTotalExperience(int exp) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getExhaustion() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setExhaustion(float value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getSaturation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSaturation(float value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getFoodLevel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFoodLevel(int value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Location getBedSpawnLocation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setBedSpawnLocation(Location location) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setBedSpawnLocation(Location location, boolean force) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean getAllowFlight() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAllowFlight(boolean flight) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void hidePlayer(Player player) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showPlayer(Player player) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean canSee(Player player) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isOnGround() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isFlying() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFlying(boolean value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFlySpeed(float value) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setWalkSpeed(float value) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getFlySpeed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getWalkSpeed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTexturePack(String url) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setResourcePack(String url) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Scoreboard getScoreboard() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isHealthScaled() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setHealthScaled(boolean scale) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setHealthScale(double scale) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getHealthScale() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Entity getSpectatorTarget() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSpectatorTarget(Entity entity) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendTitle(String title, String subtitle) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void resetTitle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, Location location, int count) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, double x, double y, double z, int count) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Spigot spigot() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getName() {
            return username;
        }

        @Override
        public PlayerInventory getInventory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Inventory getEnderChest() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setWindowProperty(Property prop, int value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InventoryView getOpenInventory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InventoryView openInventory(Inventory inventory) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InventoryView openWorkbench(Location location, boolean force) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InventoryView openEnchanting(Location location, boolean force) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void openInventory(InventoryView inventory) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InventoryView openMerchant(Villager trader, boolean force) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void closeInventory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ItemStack getItemInHand() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setItemInHand(ItemStack item) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ItemStack getItemOnCursor() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setItemOnCursor(ItemStack item) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSleeping() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getSleepTicks() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public GameMode getGameMode() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setGameMode(GameMode mode) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isBlocking() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getExpToLevel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getEyeHeight() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getEyeHeight(boolean ignoreSneaking) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Location getEyeLocation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int maxDistance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getRemainingAir() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setRemainingAir(int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMaximumAir() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setMaximumAir(int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMaximumNoDamageTicks() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setMaximumNoDamageTicks(int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getLastDamage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int _INVALID_getLastDamage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setLastDamage(double damage) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void _INVALID_setLastDamage(int damage) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getNoDamageTicks() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setNoDamageTicks(int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Player getKiller() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addPotionEffect(PotionEffect effect) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addPotionEffect(PotionEffect effect, boolean force) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addPotionEffects(Collection<PotionEffect> effects) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasPotionEffect(PotionEffectType type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removePotionEffect(PotionEffectType type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Collection<PotionEffect> getActivePotionEffects() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasLineOfSight(Entity other) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean getRemoveWhenFarAway() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setRemoveWhenFarAway(boolean remove) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityEquipment getEquipment() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCanPickupItems(boolean pickup) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean getCanPickupItems() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isLeashed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Entity getLeashHolder() throws IllegalStateException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setLeashHolder(Entity holder) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AttributeInstance getAttribute(Attribute attribute) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Location getLocation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Location getLocation(Location loc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setVelocity(Vector velocity) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Vector getVelocity() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public World getWorld() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean teleport(Location location) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean teleport(Location location, TeleportCause cause) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean teleport(Entity destination) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean teleport(Entity destination, TeleportCause cause) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Entity> getNearbyEntities(double x, double y, double z) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getEntityId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getFireTicks() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMaxFireTicks() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFireTicks(int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDead() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Server getServer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Entity getPassenger() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setPassenger(Entity passenger) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean eject() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public float getFallDistance() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFallDistance(float distance) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setLastDamageCause(EntityDamageEvent event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityDamageEvent getLastDamageCause() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UUID getUniqueId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getTicksLived() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTicksLived(int value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playEffect(EntityEffect type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityType getType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isInsideVehicle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean leaveVehicle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Entity getVehicle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCustomName(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCustomName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCustomNameVisible(boolean flag) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCustomNameVisible() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setGlowing(boolean flag) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isGlowing() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<MetadataValue> getMetadata(String metadataKey) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasMetadata(String metadataKey) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeMetadata(String metadataKey, Plugin owningPlugin) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendMessage(String message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendMessage(String[] messages) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isPermissionSet(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isPermissionSet(Permission perm) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasPermission(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasPermission(Permission perm) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeAttachment(PermissionAttachment attachment) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void recalculatePermissions() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isOp() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setOp(boolean value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void damage(double amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void _INVALID_damage(int amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void damage(double amount, Entity source) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void _INVALID_damage(int amount, Entity source) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getHealth() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int _INVALID_getHealth() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setHealth(double health) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void _INVALID_setHealth(int health) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public double getMaxHealth() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int _INVALID_getMaxHealth() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setMaxHealth(double health) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void _INVALID_setMaxHealth(int health) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void resetMaxHealth() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isConversing() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void acceptConversationInput(String input) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean beginConversation(Conversation conversation) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void abandonConversation(Conversation conversation) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isOnline() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isBanned() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setBanned(boolean banned) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isWhitelisted() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setWhitelisted(boolean value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Player getPlayer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getFirstPlayed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getLastPlayed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasPlayedBefore() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, Object> serialize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendPluginMessage(Plugin source, String channel, byte[] message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<String> getListeningPluginChannels() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public MainHand getMainHand() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isGliding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setGliding(boolean arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
