package net.craftcrepper.proauth.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.craftcrepper.proauth.bukkit.exception.UserDataNotLoadedException;
import net.craftcrepper.proauth.bukkit.exception.UserNotRegisteredException;
import net.craftcrepper.proauth.bukkit.types.AuthPlayer;
import net.craftcrepper.proauth.bukkit.types.RegistrationRequestToken;
import net.craftcrepper.proauth.database.MysqlCallback;

public class AuthCommand implements Listener {

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		ProAuthBukkit instance = ProAuthBukkit.getInstance();
		AuthPlayer authPlayer = instance.getAuthPlayer(player);
		if (!authPlayer.isAuthenticated())
			event.setCancelled(true);
		if (event.getMessage().toLowerCase().startsWith("/login ")){
			event.setCancelled(true);
			if (authPlayer.isLocked()){
				player.sendMessage(ChatColor.YELLOW + "Seus dados estão sendo processados.. Aguarde um instante.");
			} else {
				try {
					if (instance.passwordVerifier.checkPassword(player, event.getMessage().substring(7))){
						authPlayer.setAuthenticated(true);
						instance.sendAuthMessage(player);
					} else player.sendMessage(ChatColor.RED + "Senha incorreta.");
				} catch (UserDataNotLoadedException e) {
					authPlayer.setLocked(true);
				} catch (UserNotRegisteredException e) {
					if (System.currentTimeMillis() - authPlayer.getUserData().getLoadtime() > 15000){
						authPlayer.setLocked(true);
					} else player.sendMessage(ChatColor.RED + "Você ainda não está registrado. Digite /register para fazer o seu cadastro.");
				}
				if (authPlayer.isLocked()){
					player.sendMessage(ChatColor.YELLOW + "Aguarde um instante...");
					new BukkitRunnable() {
						
						@Override
						public void run() {
							try{
								instance.databaseManager.loadUserData(player);
								authPlayer.setLocked(false);
								onCommand(event);
							} catch (Exception error) {
								instance.kickPlayerForError(player);
							}
						}
					}.runTaskAsynchronously(instance);
				}
			}
		} else if (event.getMessage().toLowerCase().startsWith("/register")){
			event.setCancelled(true);
			if (authPlayer.getUserData() != null && !authPlayer.getUserData().isRegistered()){
				RegistrationRequestToken t;
				if ((t = authPlayer.getRegistrationToken()) != null && t.valid()){
					instance.sendRegistrationLink(player, t);
				} else {
					authPlayer.setRegistrationToken(null);
					instance.databaseManager.createRegistrationRequest(player, new MysqlCallback<RegistrationRequestToken>() {
						@Override
						public void onResult(RegistrationRequestToken result, Throwable error) {
							if (error != null){
								instance.alert("Error while creating new registration request for player " + player.getName() + ":");
								error.printStackTrace();
								instance.kickPlayerForError(player);
							} else instance.sendRegistrationLink(player, result);
						}
					});
				}
			} else {
				player.sendMessage(ChatColor.RED + "Você já está registrado.");
			}
		} else if (event.getMessage().toLowerCase().startsWith("/recover")){
			event.setCancelled(true);
			player.sendMessage(ChatColor.YELLOW + "[?] Esqueceu sua senha? Por favor acesse o link abaixo.");
			player.sendMessage(ProAuthBukkit.websiteAddress + "recover.php");
		}
	}
	
}
