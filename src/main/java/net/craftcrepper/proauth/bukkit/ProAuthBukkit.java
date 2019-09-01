package net.craftcrepper.proauth.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import lombok.Getter;
import net.craftcrepper.proauth.bukkit.types.AuthPlayer;
import net.craftcrepper.proauth.bukkit.types.RegistrationRequestToken;
import net.craftcrepper.proauth.bukkit.types.UserData;
import net.craftcrepper.proauth.bukkit.utils.PasswordVerifier;
import net.craftcrepper.proauth.bukkit.utils.TokenGenerator;
import net.craftcrepper.proauth.database.MysqlCallback;
import net.craftcrepper.proauth.database.MysqlConfig;
import net.craftcrepper.proauth.database.MysqlManager;
import net.craftcrepper.proauth.scheduler.FutureController;
import net.craftcrepper.proauth.scheduler.FutureRunnable;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

@Getter
public class ProAuthBukkit extends JavaPlugin implements Listener, PluginMessageListener, FutureController{

	@Getter
	private static ProAuthBukkit instance;
	@Getter
	public static String websiteAddress;
	protected TokenGenerator tokenGenerator;
	protected PasswordVerifier passwordVerifier;
	protected MysqlManager mysqlManager;
	protected DatabaseManager databaseManager;
	protected HashMap<UUID, AuthPlayer> authPlayers = new HashMap<>();
	protected HashMap<UUID, Boolean> onlineMode = new HashMap<>();
	protected long lastSecureModeRequest = 0;
	
	public ProAuthBukkit(){
		
	}
	
	public void onEnable(){
		instance = this;
		
		try {
			tokenGenerator = new TokenGenerator();
			tokenGenerator.nextToken();
		} catch (UnsupportedEncodingException e){
			disable("This JVM doesn't support UTF-8 encoding");
		} catch (NoSuchAlgorithmException e) {
			disable("This JVM doesn't support MD5 hashing");
		}
			
		this.mysqlManager = new MysqlManager(loadMysqlConfig(), this);
		websiteAddress = getConfig().getString("website", "http://configure.me/");
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				Date date = new Date();
				Calendar calendar = GregorianCalendar.getInstance();
				calendar.setTime(date); 
				if (calendar.get(Calendar.HOUR_OF_DAY) == 4 && calendar.get(Calendar.MINUTE) < 10){
					for (Player player : Bukkit.getOnlinePlayers()){
						try {
							player.kickPlayer(ChatColor.YELLOW + "[Login] O serviço de login está reiniciando (Restart diário)");
						} catch (Exception e) {
							
						}
					}
					Bukkit.shutdown();
				}
			}
		}.runTaskTimer(this, 1200L, 1200L);
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				verifyJoins();
			}
		}.runTaskTimer(instance, 50L, 50L);
		
		this.passwordVerifier = new PasswordVerifier();
		this.databaseManager = new DatabaseManager();
		
		Bukkit.getPluginManager().registerEvents(this, instance);
		Bukkit.getPluginManager().registerEvents(new AuthCommand(), instance);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		Filter f = new Filter(){
			public boolean isLoggable(LogRecord line) {
				return !line.getMessage().contains("issued server command: /login");
			}
		};
		
		Bukkit.getLogger().setFilter(f);
	}
	
	private MysqlConfig loadMysqlConfig() {
		FileConfiguration conf = getConfig();
		MysqlConfig mc = new MysqlConfig(conf.getString("mysql.host"), conf.getString("mysql.password"), conf.getString("mysql.user"), conf.getString("mysql.database"));
		System.out.println("[MysqlConfig] loaded mysql configuration for database " + mc.getDatabase() + " at " + mc.getHost());
		return mc;
	}

	public void alert(String message){
		System.out.println("[ProAuth] " + message);
	}
	
	public void disable(String error){
		alert("Plugin is being disabled: " + error);
		Bukkit.getPluginManager().disablePlugin(this);
	}
	
	public void kickPlayerForError(Player player){
		BukkitRunnable r = new BukkitRunnable() {
			
			@Override
			public void run() {
				player.kickPlayer(ChatColor.RED + "Erro no servidor de login. Tente novamente em alguns instantes. \n\n" + ChatColor.AQUA + "[!] Caso este erro persista, entre em contato com a equipe do Servidor. \n@CraftCrepper\nbsbcraftplays@gmail.com");
			}
		};
		if (Bukkit.isPrimaryThread()){
			r.run();
		} else r.runTask(instance);
	}
	
	public void sendRegistrationLink(Player player, RegistrationRequestToken token){
		player.sendMessage(ChatColor.YELLOW + "Um token de registro foi gerado para você!!");
		String link = websiteAddress + "register.php?token=" + token.getToken();
		net.md_5.bungee.api.ChatColor c = net.md_5.bungee.api.ChatColor.YELLOW;
		ComponentBuilder b = new ComponentBuilder("Para continuar o processo, ").color(c).append("CLIQUE AQUI.").color(c).bold(true).event(new ClickEvent(ClickEvent.Action.OPEN_URL, link)).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique para acessar o link de registro.").color(c.AQUA).create()));
		player.spigot().sendMessage(b.create());
	}
	
	@EventHandler
	public void onLogin(AsyncPlayerPreLoginEvent event){
		try{
			getAuthPlayer(event.getUniqueId()).setJoinTime(System.currentTimeMillis());
			databaseManager.loadUserData(event.getUniqueId());
		} catch (Exception error){
			alert("An error occurred while loading userdata for " + event.getUniqueId() + ", error info:");
			error.printStackTrace();
			event.disallow(Result.KICK_OTHER, ChatColor.RED + "Erro no servidor de login. Tente novamente em alguns instantes. \n\n" + ChatColor.AQUA + "[!] Caso este erro persista, entre em contato com a equipe do Servidor. \n@CraftCrepper\nbsbcraftplays@gmail.com");
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		afterJoin(event.getPlayer(), databaseManager.getUserData(event.getPlayer()));
		teleport(event.getPlayer());

		event.getPlayer().setNoDamageTicks(Integer.MAX_VALUE); 
		for (Player player: Bukkit.getOnlinePlayers()){
			event.getPlayer().hidePlayer(player);
			player.hidePlayer(event.getPlayer());
		}
	}
	
	private void teleport(Player player){
		player.teleport(new Location(Bukkit.getWorlds().get(0), 0.5, 100, 1.5, -180, 10));
	}
	
	public void afterJoin(Player player, UserData data){
		if (data == null){
			kickPlayerForError(player);
		} else if (data.isRegistered()) {
			getAuthPlayer(player).setJoinTime(System.currentTimeMillis());
			if (data.getEnableAutoLogin() == true && data.getOnlineMode() == true){
				player.sendMessage(ChatColor.BLUE + "[!] Você habilitou o login automático, então não precisará digitar sua senha!");
				sendAuthMessage(player);
			} else { 
				player.sendMessage(ChatColor.RED + "[!] Faça login utilizando /login <senha>");
				player.sendMessage(ChatColor.RED + "[?] Esqueceu sua senha? digite /recover para mais informaçoes.");
			}
		} else {
			databaseManager.loadRegistrationRequestToken(player, new MysqlCallback<RegistrationRequestToken>() {
				@Override
				public void onResult(RegistrationRequestToken result, Throwable error) {
					if (error != null){
						kickPlayerForError(player);
					} else if (result == null){
						player.sendMessage(ChatColor.RED + "[!] Você ainda não é registrado no servidor. Digite /register para continuar.");
						player.sendMessage(ChatColor.RED + "[?] Acesse " + websiteAddress + "/register.php para obter ajuda.");
					} else {
						sendRegistrationLink(player, result);
					}
				}
			});
		}
	}
	
	public AuthPlayer getAuthPlayer(UUID uuid){
		if (!authPlayers.containsKey(uuid))
			authPlayers.put(uuid, new AuthPlayer());
		return authPlayers.get(uuid);
	}
	
	public AuthPlayer getAuthPlayer(Player player){
		if (!authPlayers.containsKey(player.getUniqueId()) && player.isOnline())
			authPlayers.put(player.getUniqueId(), new AuthPlayer());
		return authPlayers.get(player.getUniqueId());
	}
	
	public boolean isOnlineMode(Player player){
		AuthPlayer authPlayer = getAuthPlayer(player);
		if (authPlayer.getUserData() != null && authPlayer.getUserData().isRegistered())
			return authPlayer.getUserData().getOnlineMode();
		else return onlineMode.get(player.getUniqueId());
	}

	@Override
	public void onPluginMessageReceived(String channel, Player receiver, byte[] data) {
		if (channel.equalsIgnoreCase("BungeeCord")){
			ByteArrayDataInput in = ByteStreams.newDataInput(data);
			
			String subChannel = in.readUTF();
			if (subChannel.equalsIgnoreCase("AccountInfo")){
				String uniqueId = in.readUTF();
				boolean onlineMode = in.readBoolean();
				this.onlineMode.put(UUID.fromString(uniqueId), onlineMode);
				System.out.println("[AccountInfo] " + uniqueId + " is logging in with " + (onlineMode ? "online account" : "offline account"));
			}
		}
	}
	
	public void sendAuthMessage(Player player){
		long diff;
		new BukkitRunnable() {
			
			@Override
			public void run() {		
				player.sendMessage(ChatColor.GREEN + "Você foi autenticado com sucesso.");
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("AuthPlayer");
				out.writeUTF(player.getUniqueId().toString());
				player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
			}
		}.runTaskLater(instance, (diff = System.currentTimeMillis() - getAuthPlayer(player).getJoinTime()) > 3000 ? 0 : ((3000 - diff) / 1000) * 20);
		// Delays to run only after 3 secs after joining.
	}
	
	private void verifyJoins(){
		int unregisteredPlayers = 0;
		int x = 0;
		
		Set<UUID> toRemove = new HashSet<>();
		
		for (UUID player : authPlayers.keySet()){
			AuthPlayer pl = getAuthPlayer(player);
			if (pl.getJoinTime() != null && (pl.getJoinTime() + 60000) >= System.currentTimeMillis()){
				if (pl.getUserData() == null || pl.getUserData().isRegistered()){

				} else {
					unregisteredPlayers++;
				}
			} else if (Bukkit.getPlayer(player) == null || !Bukkit.getPlayer(player).isOnline()) {
				toRemove.add(player);
			}
			x++;
		}
		
		for (UUID player : toRemove){
			authPlayers.remove(player);
			onlineMode.remove(player);
		}

	}
    

    @Override    /* Async scheduller implementation for Bukkit. */
    public void runAsync(FutureRunnable run) {
        new BukkitRunnable(){
            @Override
            public void run() {
                run.run();
            }
        }.runTaskAsynchronously(this);
    }
	
}
