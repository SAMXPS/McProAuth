package net.craftcrepper.proauth.bungee.auth;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import net.craftcrepper.proauth.bungee.ProAuthBungee;
import net.craftcrepper.proauth.database.MysqlCallback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AuthManager {

	private HashMap<ProxiedPlayer, Long> authenticatedPlayers = new HashMap<>();
	
	public AuthManager(){
		ProxyServer.getInstance().getScheduler().schedule(ProAuthBungee.instance, new Runnable() {
			
			@Override
			public void run() {
				for (ProxiedPlayer player : authenticatedPlayers.keySet()){
					try{
						if (player.getServer().getInfo().getName().equalsIgnoreCase(ProAuthBungee.instance.getConfig().getString("login-server")))
							sendToLobby(player);
						if (!player.isConnected())
							ProxyServer.getInstance().getScheduler().schedule(ProAuthBungee.instance, new Runnable() {
								
								@Override
								public void run() {
									authenticatedPlayers.remove(player);
								}
							}, 1, TimeUnit.SECONDS);
						
					} catch (Exception e) {
						
					}
				}
			}
		}, 10L, TimeUnit.SECONDS);
	}
	
	public void onAuth(ProxiedPlayer player){
		if (!authenticatedPlayers.containsKey(player)){
			authenticatedPlayers.put(player, System.currentTimeMillis());
			update(player);
		}
		sendToLobby(player);
	}
	
	private void sendToLobby(ProxiedPlayer player){
		player.connect(ProxyServer.getInstance().getServerInfo(ProAuthBungee.instance.getConfig().getString("default-server")));
	}
	
	public void update(ProxiedPlayer player){
		ProAuthBungee.instance.getMysqlManager().executeAsync("UPDATE users SET NICKNAME = ? WHERE UUID = ?", new MysqlCallback<Boolean>() {
			
			@Override
			public void onResult(Boolean result, Throwable error) {
				if (error != null){
					System.out.println("Error while updating " + player.getName() + "/" + player.getUUID() + " nickname on database");
					error.printStackTrace();
				}
			}
		}, player.getName(), player.getUUID());
	}
	
	public void remove(ProxiedPlayer player){
		if (isAuthenticated(player)){
			long loginTime = authenticatedPlayers.get(player);
			long onlineTime = (System.currentTimeMillis() - loginTime) / 1000;
			authenticatedPlayers.remove(player);
			ProAuthBungee.instance.getMysqlManager().executeAsync("INSERT INTO user_access (LOGIN_TYPE, UUID, IP_ADDRESS, LOGIN_TIME, ONLINE_TIME) values (?, ?, ?, ?, ?)", new MysqlCallback<Boolean>() {
				
				@Override
				public void onResult(Boolean result, Throwable error) {
					if (error != null){
						System.out.println("Error while saving " + player.getName() + " access log on database:");
						error.printStackTrace();
					}
				}
			}, 0, player.getUUID(), player.getAddress().getAddress().getHostAddress(), new Timestamp(loginTime), onlineTime);
		}
	}
	
	public boolean isAuthenticated(ProxiedPlayer player){
		return authenticatedPlayers.containsKey(player);
	}
	
	
	
}
