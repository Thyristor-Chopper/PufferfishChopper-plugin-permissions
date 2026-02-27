package io.potatogun.opperms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class PermissionCommands {
	private static final TranslatableComponent INSUFFICIENT_PERMISSIONS = new TranslatableComponent("자신이 갖고 있지 않은 권한을 관리할 수 없습니다");
	private static final TranslatableComponent OP_LEVEL_LOW = new TranslatableComponent("대상의 일부의 운영자 권한 단계가 자신보다 높습니다");
	private static final TranslatableComponent GROUP_EXISTS = new TranslatableComponent("그룹이 이미 존재합니다");
	private static final TranslatableComponent GROUP_NOT_FOUND = new TranslatableComponent("그룹이 존재하지 않습니다");
	private static final TranslatableComponent NO_TARGETS = new TranslatableComponent("대상이 없습니다");
	private static final TranslatableComponent GROUP_JOIN_LEAVE_FAIL = new TranslatableComponent("변동사항이 없거나 대상의 일부의 운영자 권한 단계가 자신보다 높습니다");
	private static final TranslatableComponent INVALID_PERMISSION_NAME = new TranslatableComponent("권한 이름이 올바르지 않습니다");
	
	private static boolean hasPermissions(CommandSourceStack source, String permission) {
		return source.getBukkitSender().hasPermission(permission);
	}
	
	protected static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		SuggestionProvider<CommandSourceStack> ALL_GROUPS = (context, builder) -> {
			ConfigurationSection groups = Permissions.instance.config.getConfigurationSection("permission-groups");
			if(groups == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			return SharedSuggestionProvider.suggest(new ArrayList<String>(groups.getKeys(false)), builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> OP_DEFINED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("op-permissions.op-level-" + IntegerArgumentType.getInteger(context, "level"));
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> OP_GRANTED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("op-permissions.op-level-" + IntegerArgumentType.getInteger(context, "level"));
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == true)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> OP_DENIED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("op-permissions.op-level-" + IntegerArgumentType.getInteger(context, "level"));
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == false)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> GROUP_DEFINED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("permission-groups." + StringArgumentType.getString(context, "group") + ".permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> GROUP_GRANTED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("permission-groups." + StringArgumentType.getString(context, "group") + ".permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == true)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> GROUP_DENIED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("permission-groups." + StringArgumentType.getString(context, "group") + ".permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == false)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> DEFAULT_DEFINED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("default-permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> DEFAULT_GRANTED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("default-permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == true)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
	    
	    SuggestionProvider<CommandSourceStack> DEFAULT_DENIED_PERMISSIONS = (context, builder) -> {
	    	ConfigurationSection permissions = Permissions.instance.config.getConfigurationSection("default-permissions");
			if(permissions == null)
				return SharedSuggestionProvider.suggest(Arrays.asList(), builder);
			List<String> result = new ArrayList<String>();
			for(String permission : permissions.getKeys(false))
				if(permissions.getBoolean(permission) == false)
					result.add(Permissions.unescapePermissionName(permission));
			return SharedSuggestionProvider.suggest(result, builder);
	    };
		
        commandDispatcher.register(Commands.literal("permissions")
        	.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.*"))
        	
        	// 개별 플레이어 권한
        	.then(Commands.literal("grant")
        		.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.players.grant"))
        		.then(Commands.argument("targets", EntityArgument.players())
        			.then(Commands.argument("permission", StringArgumentType.greedyString())
        				.executes((CommandContext<CommandSourceStack> context) -> setPlayerPermission(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "permission"), true, "권한을 부여하였습니다")))))
        	.then(Commands.literal("deny")
        		.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.players.deny"))
        		.then(Commands.argument("targets", EntityArgument.players())
        			.then(Commands.argument("permission", StringArgumentType.greedyString())
        				.executes((CommandContext<CommandSourceStack> context) -> setPlayerPermission(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "permission"), false, "권한을 거부하였습니다")))))
        	.then(Commands.literal("revoke")
        		.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.players.revoke"))
        		.then(Commands.argument("targets", EntityArgument.players())
        			.then(Commands.argument("permission", StringArgumentType.greedyString())
        				.executes((CommandContext<CommandSourceStack> context) -> setPlayerPermission(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "permission"), null, "권한 설정을 삭제하였습니다")))))
        	
        	// 오피 권한
    		.then(Commands.literal("op")
        		.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.op.*"))
    			.then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
					.then(Commands.literal("grant")
		        		.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.op.grant"))
		    			.then(Commands.argument("permission", StringArgumentType.greedyString())
		    				.suggests(OP_DENIED_PERMISSIONS)
		    				.executes((CommandContext<CommandSourceStack> context) -> setOpPermission(context.getSource(), IntegerArgumentType.getInteger(context, "level"), StringArgumentType.getString(context, "permission"), true, "권한을 부여하였습니다"))))
	    			.then(Commands.literal("deny")
			        	.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.op.deny"))
	    				.then(Commands.argument("permission", StringArgumentType.greedyString())
	    					.suggests(OP_GRANTED_PERMISSIONS)
	    					.executes((CommandContext<CommandSourceStack> context) -> setOpPermission(context.getSource(), IntegerArgumentType.getInteger(context, "level"), StringArgumentType.getString(context, "permission"), false, "권한을 거부하였습니다"))))
	    			.then(Commands.literal("revoke")
			        	.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.op.revoke"))
	    				.then(Commands.argument("permission", StringArgumentType.greedyString())
	    					.suggests(OP_DEFINED_PERMISSIONS)
	    					.executes((CommandContext<CommandSourceStack> context) -> setOpPermission(context.getSource(), IntegerArgumentType.getInteger(context, "level"), StringArgumentType.getString(context, "permission"), null, "권한 설정을 삭제하였습니다")))))
    		)
        	
    		// 그룹 권한
    		.then(Commands.literal("group")
    				.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.*"))
    			.then(Commands.literal("create")
		        	.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.create"))
    				.then(Commands.argument("id", StringArgumentType.string())
    					.executes((CommandContext<CommandSourceStack> context) -> createGroup(context.getSource(), StringArgumentType.getString(context, "id"), null))
    					.then(Commands.argument("name", StringArgumentType.greedyString())
    						.executes((CommandContext<CommandSourceStack> context) -> createGroup(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "name"))))))
    			.then(Commands.literal("delete")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.delete"))
    				.then(Commands.argument("group", StringArgumentType.string())
    					.suggests(ALL_GROUPS)
    					.executes((CommandContext<CommandSourceStack> context) -> deleteGroup(context.getSource(), StringArgumentType.getString(context, "group")))))
    			.then(Commands.literal("rename")
    		        	.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.rename"))
    				.then(Commands.argument("group", StringArgumentType.string())
        				.suggests(ALL_GROUPS)
    					.then(Commands.argument("name", StringArgumentType.greedyString())
    						.executes((CommandContext<CommandSourceStack> context) -> renameGroup(context.getSource(), StringArgumentType.getString(context, "group"), StringArgumentType.getString(context, "name"))))))
    			.then(Commands.literal("join")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.join"))
    				.then(Commands.argument("targets", EntityArgument.players())
    					.then(Commands.argument("group", StringArgumentType.string())
    	    				.suggests(ALL_GROUPS)
    						.executes((CommandContext<CommandSourceStack> context) -> joinGroup(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "group"))))))
    			.then(Commands.literal("leave")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.leave"))
					.then(Commands.argument("targets", EntityArgument.players())
    					.then(Commands.argument("group", StringArgumentType.string())
    	        			.suggests(ALL_GROUPS)
    						.executes((CommandContext<CommandSourceStack> context) -> leaveGroup(context.getSource(), EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "group"))))))
    			.then(Commands.literal("grant")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.grant"))
    				.then(Commands.argument("group", StringArgumentType.string())
        				.suggests(ALL_GROUPS)
    					.then(Commands.argument("permission", StringArgumentType.greedyString())
    						.suggests(GROUP_DENIED_PERMISSIONS)
    						.executes((CommandContext<CommandSourceStack> context) -> setGroupPermission(context.getSource(), StringArgumentType.getString(context, "group"), StringArgumentType.getString(context, "permission"), true, "권한을 부여하였습니다")))))
    			.then(Commands.literal("deny")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.deny"))
    				.then(Commands.argument("group", StringArgumentType.string())
            			.suggests(ALL_GROUPS)
    					.then(Commands.argument("permission", StringArgumentType.greedyString())
    						.suggests(GROUP_GRANTED_PERMISSIONS)
    						.executes((CommandContext<CommandSourceStack> context) -> setGroupPermission(context.getSource(), StringArgumentType.getString(context, "group"), StringArgumentType.getString(context, "permission"), false, "권한을 거부하였습니다")))))
    			.then(Commands.literal("revoke")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.groups.revoke"))
    				.then(Commands.argument("group", StringArgumentType.string())
            			.suggests(ALL_GROUPS)
    					.then(Commands.argument("permission", StringArgumentType.greedyString())
    						.suggests(GROUP_DEFINED_PERMISSIONS)
    						.executes((CommandContext<CommandSourceStack> context) -> setGroupPermission(context.getSource(), StringArgumentType.getString(context, "group"), StringArgumentType.getString(context, "permission"), null, "권한 설정을 삭제하였습니다")))))
    		)
    			
    		// 기본 권한
    		.then(Commands.literal("default")
    			.requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.default.*"))
    			.then(Commands.literal("allow")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.default.allow"))
					.then(Commands.argument("permission", StringArgumentType.greedyString())
						.suggests(DEFAULT_DENIED_PERMISSIONS)
						.executes((CommandContext<CommandSourceStack> context) -> setDefaultPermission(context.getSource(), StringArgumentType.getString(context, "permission"), true, "권한을 허용하였습니다"))))
    			.then(Commands.literal("deny")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.default.deny"))
					.then(Commands.argument("permission", StringArgumentType.greedyString())
						.suggests(DEFAULT_GRANTED_PERMISSIONS)
						.executes((CommandContext<CommandSourceStack> context) -> setDefaultPermission(context.getSource(), StringArgumentType.getString(context, "permission"), false, "권한을 거부하였습니다"))))
    			.then(Commands.literal("delete")
    		        .requires((CommandSourceStack source) -> hasPermissions(source, "manage-permissions.default.delete"))
					.then(Commands.argument("permission", StringArgumentType.greedyString())
						.suggests(DEFAULT_DEFINED_PERMISSIONS)
						.executes((CommandContext<CommandSourceStack> context) -> setDefaultPermission(context.getSource(), StringArgumentType.getString(context, "permission"), null, "권한 설정을 삭제하였습니다"))))
    		)
    	);
    }
	
	private static int setPlayerPermission(CommandSourceStack source, Collection<ServerPlayer> targets, String permission, Object value, String message) throws CommandSyntaxException {
		if(targets.size() <= 0)
			throw new SimpleCommandExceptionType(NO_TARGETS).create();
		if(!permission.replaceAll("[a-zA-Z0-9.*_-]", "").isEmpty() || permission.contains("..") || permission.contains("**") || permission.startsWith(".") || permission.endsWith("."))
			throw new SimpleCommandExceptionType(INVALID_PERMISSION_NAME).create();
		CommandSender sender = source.getBukkitSender();
		if(!sender.hasPermission(permission))
			throw new SimpleCommandExceptionType(INSUFFICIENT_PERMISSIONS).create();
		PlayerList playerList = source.getServer().getPlayerList();
		int myOpLevel = 0;
        if(sender instanceof Player)
            myOpLevel = ((Player) sender).getOpLevel();
        else if(sender instanceof ConsoleCommandSender)
            myOpLevel = 5;
		int i = 0;
		for(ServerPlayer player : targets) {
			if(playerList.getOpLevel(player.getGameProfile()) < myOpLevel) {
				Permissions.instance.config.set("player-permissions." + player.getUUID() + "." + Permissions.escapePermissionName(permission), value);
				i++;
			}
		}
		if(i == 0)
			throw new SimpleCommandExceptionType(OP_LEVEL_LOW).create();
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		if(i == 1)
			source.sendSuccess(new TranslatableComponent("%s(으)로 '%s' %s", targets.iterator().next().getName(), permission, message), true);
		else
			source.sendSuccess(new TranslatableComponent("%s명으로 '%s' %s", i, permission, message), true);
		return i;
	}
	
	private static int setOpPermission(CommandSourceStack source, int level, String permission, Object value, String message) throws CommandSyntaxException {
		if(!permission.replaceAll("[a-zA-Z0-9.*_-]", "").isEmpty() || permission.contains("..") || permission.contains("**") || permission.startsWith(".") || permission.endsWith("."))
			throw new SimpleCommandExceptionType(INVALID_PERMISSION_NAME).create();
		CommandSender sender = source.getBukkitSender();
		if(!sender.hasPermission(permission))
			throw new SimpleCommandExceptionType(INSUFFICIENT_PERMISSIONS).create();
		int myOpLevel = 0;
        if(sender instanceof Player)
            myOpLevel = ((Player) sender).getOpLevel();
        else if(sender instanceof ConsoleCommandSender)
            myOpLevel = 5;
        if(level >= myOpLevel)
        	throw new SimpleCommandExceptionType(OP_LEVEL_LOW).create();
		Permissions.instance.config.set("op-permissions.op-level-" + level + "." + Permissions.escapePermissionName(permission), value);
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		source.sendSuccess(new TranslatableComponent("%s단계 운영자로 '%s' %s", level, permission, message), true);
		return 1;
	}
	
	private static int setGroupPermission(CommandSourceStack source, String group, String permission, Object value, String message) throws CommandSyntaxException {
		if(!permission.replaceAll("[a-zA-Z0-9.*_-]", "").isEmpty() || permission.contains("..") || permission.contains("**") || permission.startsWith(".") || permission.endsWith("."))
			throw new SimpleCommandExceptionType(INVALID_PERMISSION_NAME).create();
		CommandSender sender = source.getBukkitSender();
		if(!sender.hasPermission(permission))
			throw new SimpleCommandExceptionType(INSUFFICIENT_PERMISSIONS).create();
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + group) == null)
			throw new SimpleCommandExceptionType(GROUP_NOT_FOUND).create();
		final String groupName = Permissions.instance.config.getString("permission-groups." + group + ".name", group);
		Permissions.instance.config.set("permission-groups." + group + ".permissions." + Permissions.escapePermissionName(permission), value);
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		source.sendSuccess(new TranslatableComponent("%s 그룹으로 '%s' %s", groupName, permission, message), true);
		return 1;
	}
	
	private static int setDefaultPermission(CommandSourceStack source, String permission, Object value, String message) throws CommandSyntaxException {
		if(!permission.replaceAll("[a-zA-Z0-9.*_-]", "").isEmpty() || permission.contains("..") || permission.contains("**") || permission.startsWith(".") || permission.endsWith("."))
			throw new SimpleCommandExceptionType(INVALID_PERMISSION_NAME).create();
		CommandSender sender = source.getBukkitSender();
		if(!sender.hasPermission(permission))
			throw new SimpleCommandExceptionType(INSUFFICIENT_PERMISSIONS).create();
		Permissions.instance.config.set("default-permissions." + Permissions.escapePermissionName(permission), value);
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		source.sendSuccess(new TranslatableComponent("기본 권한 '%s' %s", permission, message), true);
		return 1;
	}
	
	private static int createGroup(CommandSourceStack source, String id, String name) throws CommandSyntaxException {
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + id) != null)
			throw new SimpleCommandExceptionType(GROUP_EXISTS).create();
		Permissions.instance.config.createSection("permission-groups." + id);
		if(name != null)
			Permissions.instance.config.set("permission-groups." + id + ".name", name);
		else
			name = id;
		Permissions.instance.config.set("permission-groups." + id + ".permissions", new ArrayList<String>());
		Permissions.instance.saveConfig();
		source.sendSuccess(new TranslatableComponent("'%s' 권한 그룹을 만들었습니다", name), true);
		return 1;
	}
	
	private static int renameGroup(CommandSourceStack source, String id, String newName) throws CommandSyntaxException {
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + id) == null)
			throw new SimpleCommandExceptionType(GROUP_NOT_FOUND).create();
		final String groupName = Permissions.instance.config.getString("permission-groups." + id + ".name", id);
		if(newName == null)
			newName = id;
		Permissions.instance.config.set("permission-groups." + id + ".name", newName);
		Permissions.instance.saveConfig();
		source.sendSuccess(new TranslatableComponent("'%s' 권한 그룹의 이름을 '%s'(으)로 변경하였습니다", groupName, newName), true);
		return 1;
	}
	
	private static int deleteGroup(CommandSourceStack source, String id) throws CommandSyntaxException {
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + id) == null)
			throw new SimpleCommandExceptionType(GROUP_NOT_FOUND).create();
		final String groupName = Permissions.instance.config.getString("permission-groups." + id + ".name", id);
		Permissions.instance.config.set("permission-groups." + id, null);
		ConfigurationSection players = Permissions.instance.config.getConfigurationSection("player-groups");
		if(players != null)
			for(String uuid : players.getKeys(false)) {
				List<String> perms = players.getStringList(uuid);
				if(perms.contains(id)) {
					perms.remove(id);
					players.set(uuid, perms);
				}
			}
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		source.sendSuccess(new TranslatableComponent("'%s' 권한 그룹을 삭제하였습니다", groupName), true);
		return 1;
	}
	
	private static int joinGroup(CommandSourceStack source, Collection<ServerPlayer> targets, String group) throws CommandSyntaxException {
		if(targets.size() <= 0)
			throw new SimpleCommandExceptionType(NO_TARGETS).create();
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + group) == null)
			throw new SimpleCommandExceptionType(GROUP_NOT_FOUND).create();
		final String groupName = Permissions.instance.config.getString("permission-groups." + group + ".name", group);
		CommandSender sender = source.getBukkitSender();
		PlayerList playerList = source.getServer().getPlayerList();
		int myOpLevel = 0;
        if(sender instanceof Player)
            myOpLevel = ((Player) sender).getOpLevel();
        else if(sender instanceof ConsoleCommandSender)
            myOpLevel = 5;
		int i = 0;
		for(ServerPlayer player : targets) {
			if(playerList.getOpLevel(player.getGameProfile()) < myOpLevel) {
				final UUID uuid = player.getUUID();
				if(!Permissions.instance.config.contains("player-groups." + uuid))
					Permissions.instance.config.set("player-groups." + uuid, new ArrayList<String>());
				List<String> groups = Permissions.instance.config.getStringList("player-groups." + uuid);
				if(!groups.contains(group)) {
					groups.add(group);
					Permissions.instance.config.set("player-groups." + uuid, groups);
					i++;
				}
			}
		}
		if(i == 0)
			throw new SimpleCommandExceptionType(GROUP_JOIN_LEAVE_FAIL).create();
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		if(i == 1)
			source.sendSuccess(new TranslatableComponent("%s을(를) %s 그룹에 추가했습니다", targets.iterator().next().getName(), groupName), true);
		else
			source.sendSuccess(new TranslatableComponent("%s명을 %s 그룹에 추가했습니다", i, groupName), true);
		return i;
	}
	
	private static int leaveGroup(CommandSourceStack source, Collection<ServerPlayer> targets, String group) throws CommandSyntaxException {
		if(targets.size() <= 0)
			throw new SimpleCommandExceptionType(NO_TARGETS).create();
		if(Permissions.instance.config.getConfigurationSection("permission-groups." + group) == null)
			throw new SimpleCommandExceptionType(GROUP_NOT_FOUND).create();
		final String groupName = Permissions.instance.config.getString("permission-groups." + group + ".name", group);
		CommandSender sender = source.getBukkitSender();
		PlayerList playerList = source.getServer().getPlayerList();
		int myOpLevel = 0;
        if(sender instanceof Player)
            myOpLevel = ((Player) sender).getOpLevel();
        else if(sender instanceof ConsoleCommandSender)
            myOpLevel = 5;
		int i = 0;
		for(ServerPlayer player : targets) {
			if(playerList.getOpLevel(player.getGameProfile()) < myOpLevel) {
				final UUID uuid = player.getUUID();
				if(Permissions.instance.config.contains("player-groups." + uuid)) {
					List<String> groups = Permissions.instance.config.getStringList("player-groups." + uuid);
					if(groups.contains(group)) {
						groups.remove(group);
						Permissions.instance.config.set("player-groups." + uuid, groups);
						i++;
					}
				}
			}
		}
		if(i == 0)
			throw new SimpleCommandExceptionType(GROUP_JOIN_LEAVE_FAIL).create();
		Permissions.instance.saveConfig();
		Permissions.instance.refreshPermissions();
		if(i == 1)
			source.sendSuccess(new TranslatableComponent("%s을(를) %s 그룹에서 뺐습니다", targets.iterator().next().getName(), groupName), true);
		else
			source.sendSuccess(new TranslatableComponent("%s명을 %s 그룹에서 뺐습니다", i, groupName), true);
		return i;
	}
}
