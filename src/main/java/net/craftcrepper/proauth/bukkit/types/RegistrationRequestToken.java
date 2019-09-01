package net.craftcrepper.proauth.bukkit.types;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RegistrationRequestToken {

	private String uUID;
	private String nickname;
	private String token;
	private Timestamp expire;
	private boolean onlineMode;
	
	public Object[] toFormatter(){
		return new Object[]{getUUID(), getNickname(), getToken(), getExpire(), isOnlineMode()};
	}
	
	public boolean valid(){
		return expire.after(new Timestamp(System.currentTimeMillis()));
	}
	
}
