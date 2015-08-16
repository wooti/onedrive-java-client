package com.wouterbreukink.onedrive.encryption;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
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
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
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
	
	public void decryptFile(File aCipherText, File aPlainText) throws IOException, EncryptionException
	{
		if (aCipherText.length() < 48)
		{
			throw new EncryptionException();
		}
		try
		{
			InputStream is = new BufferedInputStream(new FileInputStream(aCipherText));
			OutputStream os = new BufferedOutputStream(new FileOutputStream(aPlainText));
			byte[] header = new byte[32];
			is.read(header);		
			byte[] lIV = Arrays.copyOfRange(header,0,16);
			byte[] lSalt = Arrays.copyOfRange(header,16,32);
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec mSecretKeySpec = new SecretKeySpec(lSaltedPassword, "AES");
			theCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, new IvParameterSpec(lIV));
			CipherOutputStream cos = new CipherOutputStream(os, theCipher);
			doCopy(is, cos);
		}
		catch (InvalidKeyException | InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			System.exit(1);			
		}		
	}

	public DataInputStream encryptFileToStream(File aPlainText) throws FileNotFoundException
	{
		try
		{
			InputStream is = new BufferedInputStream(new FileInputStream(aPlainText));
			
			byte[] lSalt = generateSalt();
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, "AES");			
			theCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = theCipher.getIV();
			
			@SuppressWarnings("resource")
			DataInputStream ret =
				new DataInputStream(
					new SequenceInputStream(
							new SequenceInputStream(
									new ByteArrayInputStream(lIV),
									new ByteArrayInputStream(lSalt)),
							new CipherInputStream(is, theCipher)));
			return ret;
		}
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			System.exit(1);
			return null;
		}		
	}
	
	private void doCopy(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[1*1024*1024];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			os.write(bytes, 0, numBytes);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	/*
public static void encryptOrDecrypt(String key, int mode, InputStream is, OutputStream os) throws Throwable {

		DESKeySpec dks = new DESKeySpec(key.getBytes());
		SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
		SecretKey desKey = skf.generateSecret(dks);
		Cipher cipher = Cipher.getInstance("DES"); // DES/ECB/PKCS5Padding for SunJCE

		if (mode == Cipher.ENCRYPT_MODE) {
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
			CipherInputStream cis = new CipherInputStream(is, cipher);
			doCopy(cis, os);
		} else if (mode == Cipher.DECRYPT_MODE) {
			cipher.init(Cipher.DECRYPT_MODE, desKey);
			CipherOutputStream cos = new CipherOutputStream(os, cipher);
			doCopy(is, cos);
		}
	}

	public static void doCopy(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[64];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			os.write(bytes, 0, numBytes);
		}
		os.flush();
		os.close();
		is.close();
	}
	 
	 */
	
	
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
