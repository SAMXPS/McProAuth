package net.craftcrepper.proauth.bukkit.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserData {

	private boolean registered;
	private String uUID;
	private String email;
	private String hashedPassword;
	private Boolean onlineMode;
	private Boolean enableAutoLogin;
	private final long loadtime = System.currentTimeMillis();
	
	public static UserData getUnregisteredData(String uuid){
		return new UserData(false, uuid, null, null, null, null);
	}
	
}
