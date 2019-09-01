package net.craftcrepper.proauth.bukkit.utils;

import java.util.UUID;

public class UtilUUID {

	public static String getNoDashesUUID(UUID uuid){
		return uuid.toString().replaceAll("-", "");
	}
	
}
