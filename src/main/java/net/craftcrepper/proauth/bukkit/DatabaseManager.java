package net.craftcrepper.proauth.bukkit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import net.craftcrepper.proauth.bukkit.types.UserData;
import net.craftcrepper.proauth.bukkit.utils.UtilUUID;
import net.craftcrepper.proauth.database.MysqlCallback;
import net.craftcrepper.proauth.bukkit.types.RegistrationRequestToken;


public class DatabaseManager {
	
	private ProAuthBukkit main = ProAuthBukkit.getInstance();
	private final String[] prepare = {
		"CREATE TABLE IF NOT EXISTS `users` (   `ID` int(11) NOT NULL AUTO_INCREMENT,   `UUID` varchar(40) NOT NULL,   `NICKNAME` varchar(32) NOT NULL,   `EMAIL` varchar(256) NOT NULL,   `PASSWORD` varchar(256) NOT NULL,   `ONLINE_MODE` tinyint(1) NOT NULL,   `EMAIL_VERIFIED` tinyint(1) NOT NULL DEFAULT '1',   `AUTO_LOGIN` tinyint(1) NOT NULL DEFAULT '0',   PRIMARY KEY (`ID`),   UNIQUE KEY `UUID` (`UUID`),   UNIQUE KEY `ID` (`ID`),   UNIQUE KEY `EMAIL` (`EMAIL`) ) ENGINE=MyISAM AUTO_INCREMENT=10250 DEFAULT CHARSET=latin1;",
		"CREATE TABLE IF NOT EXISTS `recover_request` (   `ID` int(11) NOT NULL AUTO_INCREMENT,   `UUID` varchar(36) NOT NULL,   `USED` tinyint(1) NOT NULL,   `TOKEN` varchar(32) NOT NULL,   `EXPIRE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,   `IP_ADDRESS` varchar(45) NOT NULL,   `CREATION` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,   PRIMARY KEY (`ID`),   UNIQUE KEY `ID` (`ID`),   UNIQUE KEY `TOKEN` (`TOKEN`) ) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=latin1;",
		"CREATE TABLE IF NOT EXISTS `registration_request` (   `ID` int(11) NOT NULL AUTO_INCREMENT,   `UUID` varchar(36) NOT NULL,   `NICKNAME` varchar(32) NOT NULL,   `TOKEN` varchar(32) NOT NULL,   `EXPIRE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,   `ONLINE_MODE` tinyint(1) NOT NULL,   PRIMARY KEY (`ID`),   UNIQUE KEY `ID` (`ID`),   UNIQUE KEY `TOKEN` (`TOKEN`) ) ENGINE=MyISAM AUTO_INCREMENT=45912 DEFAULT CHARSET=latin1; ",
	};
	
	public void init() {
		for (String prepare : this.prepare) {
			main.getMysqlManager().executeAsync(prepare, new MysqlCallback<Boolean>() {
				@Override
				public void onResult(Boolean paramT, Throwable paramThrowable) {
					if (paramThrowable != null) {
						main.getLogger().log(Level.SEVERE, "Can't create database table");
					} else if (paramT) {
						// Ok, everything went as expected
					}
				}
			});
		}
	}

	public UserData getUserData(Player player){
		return main.getAuthPlayer(player).getUserData();
	}
	
	public synchronized UserData loadUserData(Player player) throws SQLException{
		return loadUserData(player.getUniqueId());
	}
	
	public synchronized UserData loadUserData(UUID uuid) throws SQLException{
		UserData result = loadUserData(UtilUUID.getNoDashesUUID(uuid));
		main.getAuthPlayer(uuid).setUserData(result);
		return result;
	}
	
	public synchronized UserData loadUserData(String uuid) throws SQLException{
		ResultSet result = main.mysqlManager.executeQuery("SELECT PASSWORD, ONLINE_MODE, EMAIL, AUTO_LOGIN FROM users WHERE UUID = ?;", uuid); 
		result.beforeFirst();
		return !result.next() ? UserData.getUnregisteredData(uuid) : new UserData(true, uuid, result.getString("EMAIL"), result.getString("PASSWORD"), result.getBoolean("ONLINE_MODE"), result.getBoolean("AUTO_LOGIN"));
	}
	
	public void loadRegistrationRequestToken(Player player, MysqlCallback<RegistrationRequestToken> callback){
		loadRegistrationRequestToken(UtilUUID.getNoDashesUUID(player.getUniqueId()), new MysqlCallback<RegistrationRequestToken>() {
			
			@Override
			public void onResult(RegistrationRequestToken result, Throwable error) {
				if (error != null){
					main.alert("An error occurred while loading registration tokens for " + player.getName() + ", error info:");
					error.printStackTrace();
				} else if (player.isOnline() && result != null){
					main.getAuthPlayer(player).setRegistrationToken(result);
				}
				callback.onResult(result, error);
			}			
		});
	}
	
	public void loadRegistrationRequestToken(String uuid, MysqlCallback<RegistrationRequestToken> callback){
		main.mysqlManager.executeQueryAsync("SELECT NICKNAME, TOKEN, EXPIRE, ONLINE_MODE FROM registration_request WHERE UUID = ? AND EXPIRE > CURRENT_TIMESTAMP LIMIT 1;", new MysqlCallback<ResultSet>() {

			@Override
			public void onResult(ResultSet result, Throwable error) {
				if (error != null){
					callback.onResult(null, error);
				} else {
					try {
						result.beforeFirst();
						callback.onResult(!result.next() ? null : new RegistrationRequestToken(uuid, result.getString("NICKNAME"), result.getString("TOKEN"), result.getTimestamp("EXPIRE"), result.getBoolean("ONLINE_MODE")));
					} catch (SQLException e) {
						callback.onResult(null, e);
					}
				}
			}
			
		}, uuid);
	}
	
	public void createRegistrationRequest(Player player, MysqlCallback<RegistrationRequestToken> callback){
		try {
			RegistrationRequestToken newToken = new RegistrationRequestToken(UtilUUID.getNoDashesUUID(player.getUniqueId()), player.getName(), main.tokenGenerator.nextToken(), new Timestamp(System.currentTimeMillis() + 60 * 60 * 1000), main.isOnlineMode(player));
			main.mysqlManager.executeAsync("INSERT INTO `registration_request`(`UUID`, `NICKNAME`, `TOKEN`, `EXPIRE`, `ONLINE_MODE`) VALUES (?,?,?,?,?);", new MysqlCallback<Boolean>() {

				@Override
				public void onResult(Boolean result, Throwable error) {
					callback.onResult(newToken, error);
				}
			
			}, newToken.toFormatter());
		} catch (Exception e) {
			callback.onResult(null, e);
		}
	}
	
}
