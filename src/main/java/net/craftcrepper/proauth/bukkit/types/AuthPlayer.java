package net.craftcrepper.proauth.bukkit.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
@Setter
public class AuthPlayer {

	private UserData userData = null;
	private RegistrationRequestToken registrationToken;
	private boolean locked;
	private boolean onlineMode;
	public boolean authenticated = false;
	private Long joinTime;
	
}
