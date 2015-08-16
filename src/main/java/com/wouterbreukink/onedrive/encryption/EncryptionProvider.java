package com.wouterbreukink.onedrive.encryption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionProvider 
{
	private SecureRandom theSecureRandom;
	private SecretKeyFactory theSecretKeyFactory;
	private Cipher theCipher;
	private String theKey;
	
	private static final int ITERATIONS = 16384;
	private static final int KEY_LENGTH = 128;
	private static final int SALT_LENGTH = 16;
	
	public EncryptionProvider(String aKey)
	{
		try 
		{
			theSecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			theCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException e) 
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		theSecureRandom = new SecureRandom();
		theKey = aKey;
	}
	
	public String encryptFilename(String aPlainText)
	{	
		try 
		{
			byte[] lSalt = generateSalt();
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, "AES");	    
			theCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = theCipher.getIV();
			byte[] lEncryptedMessage = theCipher.doFinal(aPlainText.getBytes());
			byte[] lCipherText = new byte[lIV.length + lSalt.length + lEncryptedMessage.length];       
			System.arraycopy(lIV, 0, lCipherText, 0, lIV.length);
			System.arraycopy(lSalt, 0, lCipherText, lIV.length, lSalt.length);
			System.arraycopy(lEncryptedMessage, 0, lCipherText, lIV.length + lSalt.length, lEncryptedMessage.length);
			String lCipherTextBase64 = Base64.getEncoder().encodeToString(lCipherText);
			return lCipherTextBase64.replace('/','_').replace('+','-');
		} 
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) 
		{
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	public String decryptFilename(String aCipherText) throws EncryptionException
	{
		try 
		{
			String lUnescapedCipherText = aCipherText.replace('_','/').replace('-','+');
			byte[] lCipherText;
			try
			{
				lCipherText = Base64.getDecoder().decode(lUnescapedCipherText);
			}			
			catch (IllegalArgumentException e)
			{
				throw new EncryptionException();
			}
			if (lCipherText.length < 48)
			{
				throw new EncryptionException();
			}
			byte[] lIV = Arrays.copyOfRange(lCipherText,0,16);
			byte[] lSalt = Arrays.copyOfRange(lCipherText,16,32);
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec mSecretKeySpec = new SecretKeySpec(lSaltedPassword, "AES");
			theCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, new IvParameterSpec(lIV));		
			byte[] lEncryptedMessage = Arrays.copyOfRange(lCipherText, 32, lCipherText.length);
			byte[] decryptedTextBytes = theCipher.doFinal(lEncryptedMessage);
			return new String(decryptedTextBytes);
		}
		catch (InvalidKeyException | InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			System.exit(1);
			return null;
		} 
		catch (IllegalBlockSizeException | BadPaddingException e) 
		{
			throw new EncryptionException();
		}				
	}
	
	public byte[] encryptFile(File aPlainText) throws IOException
	{	
		try 
		{
			byte[] lSalt = generateSalt();
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, "AES");	    
			theCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = theCipher.getIV();
			byte[] lEncryptedMessage = theCipher.doFinal(Files.readAllBytes(aPlainText.toPath()));
			byte[] lCipherText = new byte[lIV.length + lSalt.length + lEncryptedMessage.length];       
			System.arraycopy(lIV, 0, lCipherText, 0, lIV.length);
			System.arraycopy(lSalt, 0, lCipherText, lIV.length, lSalt.length);
			System.arraycopy(lEncryptedMessage, 0, lCipherText, lIV.length + lSalt.length, lEncryptedMessage.length);
			if (lCipherText.length != computeEncryptedLength(aPlainText.length()))
			{
				System.out.println("Unexpected ciphertext length: " +
						"EXP = " + computeEncryptedLength(aPlainText.length()) +
						" ACT = " + lCipherText.length);
				System.exit(1);
				return null;
			}
			return lCipherText;
			
		} 
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) 
		{
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public long computeEncryptedLength(long plainTextlength)
	{
		return 32 + ((plainTextlength + 16) & ~15);
	}

	private byte[] generateSalt()
	{   
	    byte saltBytes[] = new byte[SALT_LENGTH];
	    theSecureRandom.nextBytes(saltBytes);
	    return saltBytes;
	}
	
	private byte[] getSaltedPassword(String aPassword, byte[] aSalt)
	{
		try 
	    {
			KeySpec lKeySpec = new PBEKeySpec(aPassword.toCharArray(), aSalt, ITERATIONS, KEY_LENGTH);
			return theSecretKeyFactory.generateSecret(lKeySpec).getEncoded();
		} 
	    catch (InvalidKeySpecException e) 
	    {
			e.printStackTrace();			
			System.exit(1);
			return null;
		}
	}
}
