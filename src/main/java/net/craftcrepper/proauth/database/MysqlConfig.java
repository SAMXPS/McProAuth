package net.craftcrepper.proauth.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MysqlConfig {
	
	@Getter
	private final String host, password, user, database;

}
