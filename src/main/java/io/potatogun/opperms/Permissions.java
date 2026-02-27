package io.potatogun.opperms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public class Permissions extends JavaPlugin implements Listener {
	private MinecraftServer server = null;
	protected static Permissions instance = null;
	protected FileConfiguration config = null;
	private Map<UUID, PermissionAttachment> attachments = new HashMap<UUID, PermissionAttachment>();
	
	@Override
	public void onEnable() {
		instance = this;
		
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		
		server = ((CraftServer) Bukkit.getServer()).getServer();
		CommandDispatcher<CommandSourceStack> dispatcher = server.vanillaCommandDispatcher.getDispatcher();
		PermissionCommands.register(dispatcher);
		
		getServer().getPluginManager().registerEvents(this, this);
		
		refreshPermissions();
	}
	
	protected void refreshPermissions() {
		for(Player player : Bukkit.getOnlinePlayers())
			setPermissions(player);
	}
	
	private void setPermissions(Player player) {
		UUID uuid = player.getUniqueId();
		
		if(attachments.containsKey(uuid)) {
			player.removeAttachment(attachments.get(uuid));
			attachments.remove(uuid);
		}
		
		PermissionAttachment attachment = player.addAttachment(this);
		attachments.put(uuid, attachment);
		
		// 기본 권한
		ConfigurationSection defaultPermissions = config.getConfigurationSection("default-permissions");
		if(defaultPermissions != null)
			for(String permission : defaultPermissions.getKeys(false))
				attachment.setPermission(unescapePermissionName(permission), defaultPermissions.getBoolean(permission));
		
		// 오피 권한
		if(player.isOp()) {
			int opLevel = player.getOpLevel();
			int startLevel = config.getBoolean("op-permission-cascade") ? 1 : opLevel;
			for(int i=startLevel; i<=opLevel; i++) {
				ConfigurationSection opPermissions = config.getConfigurationSection("op-permissions.op-level-" + i);
				if(opPermissions != null)
					for(String permission : opPermissions.getKeys(false))
						attachment.setPermission(unescapePermissionName(permission), opPermissions.getBoolean(permission));
			}
		}
		
		// 그룹 권한
		List<String> groups = config.getStringList("player-groups." + uuid);
		for(String groupName : groups) {
			ConfigurationSection group = config.getConfigurationSection("permission-groups." + groupName + ".permissions");
			if(group != null)
				for(String permission : group.getKeys(false))
					attachment.setPermission(unescapePermissionName(permission), group.getBoolean(permission));
		}
		
		// 개별 플레이어 권한
		ConfigurationSection playerPermissions = config.getConfigurationSection("player-permissions." + uuid);
		if(playerPermissions != null)
			for(String permission : playerPermissions.getKeys(false))
				attachment.setPermission(unescapePermissionName(permission), playerPermissions.getBoolean(permission));
		
		player.recalculatePermissions();
		server.getPlayerList().sendPlayerPermissionLevel(((CraftPlayer) player).getHandle());
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
	public void onPlayerJoin(PlayerJoinEvent event) {
		setPermissions(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		if(attachments.containsKey(uuid))
			attachments.remove(uuid);
	}
	
	protected static String escapePermissionName(String name) {
		return name.replaceAll("[.]", "\\$");
	}
	
	protected static String unescapePermissionName(String name) {
		return name.replaceAll("[$]", ".");
	}
	
	@Override
	public void onDisable() {
	}
}
