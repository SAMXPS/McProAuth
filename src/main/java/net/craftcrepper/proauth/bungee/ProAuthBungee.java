package net.craftcrepper.proauth.bungee;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import net.craftcrepper.proauth.bungee.auth.AuthManager;
import net.craftcrepper.proauth.bungee.utils.MojangAPIUtils.MinecraftAccountInfo;
import net.craftcrepper.proauth.database.MysqlConfig;
import net.craftcrepper.proauth.database.MysqlManager;
import net.craftcrepper.proauth.scheduler.FutureController;
import net.craftcrepper.proauth.scheduler.FutureRunnable;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

@Getter
public class ProAuthBungee extends Plugin implements Listener, FutureController{

	public static ProAuthBungee instance;
	private Configuration config;
	private PremiumManager premiumManager;
	private AuthManager authManager;
	private MysqlManager mysqlManager;

	public void loadConfig(){
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
				Files.copy(getResourceAsStream("config.yml"), file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
		Configuration configuration;
		try {
			configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
		} catch (IOException e) {
			e.printStackTrace();
			configuration = null;
		}
		this.config = configuration;
	}
	
	public Configuration getConfig(){
		if (config == null)
			loadConfig();
		return config;
	}
	
	public void saveConfig(Configuration configuration){
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
				Files.copy(getResourceAsStream("config.yml"), file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onMessage(PluginMessageEvent event){
		if (event.getTag().equals("BungeeCord")){
		    ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
		    String subchannel = in.readUTF();
		    if (subchannel.equalsIgnoreCase("AuthPlayer")){
		    	authManager.onAuth(getProxy().getPlayer(UUID.fromString(in.readUTF())));
		    }
		}
	}
	
	public void onEnable() {
		instance = this;
		premiumManager = new PremiumManager();
		mysqlManager = new MysqlManager(loadMysqlConfig(), this);
		authManager = new AuthManager();
		
		getProxy().getPluginManager().registerListener(this, this);
	}
	
	public MysqlConfig loadMysqlConfig(){
		Configuration conf = getConfig();
		return new MysqlConfig(conf.getString("mysql.host"), conf.getString("mysql.password"), conf.getString("mysql.user"), conf.getString("mysql.database"));
	}
	
	@EventHandler
	public void onQuit(PlayerDisconnectEvent event){
		premiumManager.remove(event.getPlayer());
		authManager.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onLoginReceive(PreLoginEvent event) {
		event.registerIntent(instance);
		getProxy().getScheduler().runAsync(instance, new Runnable() {
			
			@Override
			public void run() {
				try {
					premiumManager.verifyPremium(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
				event.completeIntent(instance);
			}
		});
	}
	
	@EventHandler
	public void onServerConnect(final ServerConnectEvent event){
		if (authManager.isAuthenticated(event.getPlayer())){
			return;
		} else {
			event.setTarget(ProxyServer.getInstance().getServerInfo(config.getString("login-server")));
		}
	}
	
	@EventHandler
	public void onConnected(ServerConnectedEvent event){
		MinecraftAccountInfo info = premiumManager.getAccountInfo(event.getPlayer());
		
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("AccountInfo");
		out.writeUTF(event.getPlayer().getUniqueId().toString());
		out.writeBoolean(info.isValid() && !info.isDemo());
		event.getServer().getInfo().sendData("BungeeCord", out.toByteArray());
	}

	@EventHandler
	public void onChat(ChatEvent event){
		try{
			ProxiedPlayer pl = (ProxiedPlayer) event.getSender();
			if (!authManager.isAuthenticated(pl)){
				if (!event.getMessage().startsWith("/login") && !event.getMessage().startsWith("/register") && !event.getMessage().startsWith("/recover")){
					pl.sendMessage("Se registre/logue antes de fazer isto.");
					event.setCancelled(true);
				}
			} else {
				// Handle logged stuff
			}
		} catch (Exception e){
			
		}
		
	}

	@Override
	public void runAsync(FutureRunnable run) {
		ProxyServer.getInstance().getScheduler().runAsync(this, run);
	}

}
