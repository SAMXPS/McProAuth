package net.craftcrepper.proauth.bukkit.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.mindrot.jbcrypt.BCrypt;

public class TokenGenerator {

    private SecureRandom random = new SecureRandom();
    private String charset = "UTF-8";
    private MessageDigest m;
    
    public TokenGenerator() throws NoSuchAlgorithmException{
    	 m = MessageDigest.getInstance("MD5");
    }

    // Tested up to 23422418 tokens with no duplicates in 4 minutes of constant hashing (4GHz core/4GB RAM JVM)
    public String nextToken() throws UnsupportedEncodingException{
        return md5(new BigInteger(128, random).toString(32) + BCrypt.gensalt());
    }
    
    public String md5(String input) throws UnsupportedEncodingException{
    	return new BigInteger(1, m.digest(input.getBytes(charset))).toString(16);
    }

}
