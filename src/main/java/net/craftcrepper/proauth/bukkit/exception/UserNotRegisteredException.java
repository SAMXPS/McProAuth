package net.craftcrepper.proauth.bukkit.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UserNotRegisteredException extends Exception {

	private final String userUUID;

}
