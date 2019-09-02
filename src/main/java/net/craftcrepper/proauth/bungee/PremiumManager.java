package net.craftcrepper.proauth.bungee;

import java.util.HashMap;
import java.util.UUID;

import com.google.common.base.Charsets;

import net.craftcrepper.proauth.bungee.utils.MojangAPIUtils;
import net.craftcrepper.proauth.bungee.utils.MojangAPIUtils.MinecraftAccountInfo;
import net.craftcrepper.proauth.bungee.utils.MojangAPIUtils.TooManyRequestsException;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;

public class PremiumManager {

	private HashMap<UUID, MinecraftAccountInfo> accounts = new HashMap<>();

	public void remove(ProxiedPlayer player){
		accounts.remove(player.getUniqueId());
	}
	
	public MinecraftAccountInfo getAccountInfo(ProxiedPlayer player){
		return accounts.get(player.getUniqueId());
	}
	
	public void verifyPremium(final PreLoginEvent event) {
		if (!event.isCancelled()){
			try {
				MinecraftAccountInfo acc = MojangAPIUtils.loadAccountInfo(event.getConnection().getName());
				event.getConnection().setOnlineMode(acc.isValid() && !acc.isDemo());
				UUID uuid = acc.getId() == null ? getOfflineUUID(acc.getName()) : Util.getUUID(acc.getId());
				accounts.put(uuid, acc);
			} catch (Exception e) {
				if (e instanceof TooManyRequestsException){
					// 600 requests?? Really?
				}
				event.setCancelled(true);
				event.setCancelReason("Can't connect to Mojang servers right now. Try again in a few seconds.");
				System.out.println("[McProAuth] Erro ao verificar se a conta "
						+ event.getConnection().getName() + " possui minecraft original:");
				e.printStackTrace();
			}
		}
	}
	
	public static UUID getOfflineUUID(String name){
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
	}

}
