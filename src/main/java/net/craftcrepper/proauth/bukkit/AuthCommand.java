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
				if (instance.isWebsiteHook()){
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
					boolean requireEmail = instance.getConfig().getBoolean("require-email", false);
					String data[] = event.getMessage().replace("/register ", "").replace("/register","").split(" ");
					if (data.length >= (requireEmail ? 3 : 2)) {
						if (!data[0].equals(data[1]) ) {
							player.sendMessage(ChatColor.RED + "ERRO: As senhas não coincidem.");
						} else {
							String email = data.length >= 3 ? data[2] : "";
							player.sendMessage(ChatColor.RED + "Realizando registro... aguarde um momento.");
							instance.getDatabaseManager().registerPlayer(player, data[0], email, new MysqlCallback<Boolean>() {
								@Override
								public void onResult(Boolean data, Throwable e) {
									if (e != null) {
										instance.alert("Error while creating registering player " + player.getName() + ":");
										instance.kickPlayerForError(player);
										
										e.printStackTrace();
									} else {
										new BukkitRunnable(){
										
											@Override
											public void run() {
												player.sendMessage(ChatColor.RED + "Registro feito com sucesso. Digite /login para fazer login.");
											}
										}.runTask(instance);
										try{
											authPlayer.setLocked(true);
											instance.databaseManager.loadUserData(player);
											authPlayer.setLocked(false);
										} catch (Exception error) {
											instance.kickPlayerForError(player);
										}
									}
								}
							});
							return;
						}
					} 
					player.sendMessage(ChatColor.RED + "Use /register <senha> <senha> " + (requireEmail ? "<email>" : "[email (opcional)]"));
				}
			} else {
				player.sendMessage(ChatColor.RED + "Você já está registrado. Digite /login para fazer seu login.");
				player.sendMessage(ChatColor.RED + "Esqueceu sua senha? /recover");
			}
		} else if (event.getMessage().toLowerCase().startsWith("/recover")){
			event.setCancelled(true);
			player.sendMessage(ChatColor.YELLOW + "[?] Esqueceu sua senha? Por favor acesse o link abaixo.");
			player.sendMessage(ProAuthBukkit.websiteAddress + "recover.php");
		}
	}
	
}
