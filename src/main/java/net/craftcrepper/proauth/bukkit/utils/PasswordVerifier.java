package net.craftcrepper.proauth.bukkit.utils;

import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

import net.craftcrepper.proauth.ProAuth;
import net.craftcrepper.proauth.bukkit.ProAuthBukkit;
import net.craftcrepper.proauth.bukkit.exception.UserDataNotLoadedException;
import net.craftcrepper.proauth.bukkit.exception.UserNotRegisteredException;
import net.craftcrepper.proauth.bukkit.types.UserData;


public class PasswordVerifier {
	
	public boolean checkPassword(Player player, String input) throws UserDataNotLoadedException, UserNotRegisteredException{
		UserData data;
		if ((data = ProAuthBukkit.getInstance().getDatabaseManager().getUserData(player)) != null){
			if (data.isRegistered()){
				return checkPassword(input, data.getHashedPassword());
			} else throw new UserNotRegisteredException(UtilUUID.getNoDashesUUID(player.getUniqueId()));
		} else throw new UserDataNotLoadedException(UtilUUID.getNoDashesUUID(player.getUniqueId()));
	}
	
	public static boolean checkPassword(String plaintext, String hashed){
		return BCrypt.checkpw(plaintext, fixCompatibility(hashed));
	} 

	public static String hashPassword(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}
	
	private static String fixCompatibility(String hash){
		char[] arr = hash.toCharArray();
		switch (arr[2]) {
		case 'a':
			break;
		case 'x' | 'y':
			arr[2] = 'a';
			break;
		}
		return String.valueOf(arr);
	}
	
}
