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

import com.wouterbreukink.onedrive.CommandLineOpts;

public class EncryptionProvider 
{
	private SecureRandom theSecureRandom;
	private SecretKeyFactory theSecretKeyFactory;
	private Cipher theCipher;
	private String theKey;
	
	private static EncryptionProvider instance = null;
	
	private static final int ITERATIONS = 16384;
	private static final int KEY_LENGTH = 128;
	private static final int SALT_LENGTH = 16;
	private static final int IV_LENGTH = 16;
	private static final int PREFIX_LENGTH = SALT_LENGTH + IV_LENGTH;
	private static final int BLOCK_SIZE = 16;
	private static final int MIN_CIPHERTEXT_LENGTH = 48;
	private static final int BUFFER_SIZE = 1 * 1024 * 1024;
	

	private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String SECRET_KEY_ALGORITHM = "AES";
	
	public synchronized static EncryptionProvider getEncryptionProvider() {
        if (instance != null) {
        	if (!CommandLineOpts.getCommandLineOpts().isEncryptionEnabled())
        		throw new IllegalStateException("Encryption provider cannot be initialized when encryption is disabled");
        	instance = new EncryptionProvider(CommandLineOpts.getCommandLineOpts().getEncryptionKey());            
        }
        return instance;
    }	
	
	private EncryptionProvider(String aKey)
	{
		try 
		{
			theSecretKeyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			theCipher = Cipher.getInstance(CIPHER_ALGORITHM);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException e) 
		{
			throw new IllegalStateException("Failed to initialize encryption provider", e);
		}
		
		theSecureRandom = new SecureRandom();
		theKey = aKey;
	}
	
	public synchronized String encryptFilename(String aPlainText)
	{	
		try 
		{
			byte[] lSalt = generateSalt();			
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, SECRET_KEY_ALGORITHM);	    
			theCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = theCipher.getIV();
			byte[] lEncryptedMessage = theCipher.doFinal(aPlainText.getBytes());
			byte[] lCipherText = new byte[PREFIX_LENGTH + lEncryptedMessage.length];
			
			assert lIV.length == IV_LENGTH;
			assert lSalt.length == SALT_LENGTH;
			assert lCipherText.length == computeEncryptedLength(aPlainText.length());
			
			System.arraycopy(lIV, 0, lCipherText, 0, IV_LENGTH);
			System.arraycopy(lSalt, 0, lCipherText, IV_LENGTH, SALT_LENGTH);
			System.arraycopy(lEncryptedMessage, 0, lCipherText, PREFIX_LENGTH, lEncryptedMessage.length);
			String lCipherTextBase64 = Base64.getEncoder().encodeToString(lCipherText);
			return lCipherTextBase64.replace('/','_').replace('+','-');
		} 
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) 
		{
			throw new IllegalStateException("Invalid encryption provider configuration", e);
		}
	}

	public synchronized String decryptFilename(String aCipherText) throws EncryptionException
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
				throw new EncryptionException("Filename is not Base64-encoded" + e);
			}
			if (lCipherText.length < MIN_CIPHERTEXT_LENGTH)
			{
				throw new EncryptionException("Filename is too short");
			}
			byte[] lIV = Arrays.copyOfRange(lCipherText, 0, IV_LENGTH);
			byte[] lSalt = Arrays.copyOfRange(lCipherText, IV_LENGTH, PREFIX_LENGTH);
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec mSecretKeySpec = new SecretKeySpec(lSaltedPassword, SECRET_KEY_ALGORITHM);
			theCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, new IvParameterSpec(lIV));		
			byte[] lEncryptedMessage = Arrays.copyOfRange(lCipherText, PREFIX_LENGTH, lCipherText.length);
			byte[] decryptedTextBytes = theCipher.doFinal(lEncryptedMessage);
			return new String(decryptedTextBytes);
		}
		catch (InvalidKeyException | InvalidAlgorithmParameterException e) 
		{
			throw new IllegalStateException("Invalid encryption provider configuration", e);
		} 
		catch (IllegalBlockSizeException | BadPaddingException e) 
		{
			throw new EncryptionException("Filename cannot be decrypted using the given key", e);
		}				
	}
	
	public byte[] encryptFile(File aPlainText) throws IOException
	{	
		try 
		{
			byte[] lSalt = generateSalt();
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, SECRET_KEY_ALGORITHM);
			Cipher lCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			lCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = lCipher.getIV();
			byte[] lEncryptedMessage = lCipher.doFinal(Files.readAllBytes(aPlainText.toPath()));
			byte[] lCipherText = new byte[PREFIX_LENGTH + lEncryptedMessage.length];
			
			assert lIV.length == IV_LENGTH;
			assert lSalt.length == SALT_LENGTH;
			assert lCipherText.length == computeEncryptedLength(aPlainText.length());
			
			System.arraycopy(lIV, 0, lCipherText, 0, IV_LENGTH);
			System.arraycopy(lSalt, 0, lCipherText, IV_LENGTH, SALT_LENGTH);
			System.arraycopy(lEncryptedMessage, 0, lCipherText, PREFIX_LENGTH, lEncryptedMessage.length);
			return lCipherText;
		} 
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | 
			   NoSuchAlgorithmException | NoSuchPaddingException e) 
		{
			throw new IllegalStateException("Invalid encryption provider configuration", e);
		}
	}
	
	public void decryptFile(File aCipherText, File aPlainText) throws IOException
	{
		if (aCipherText.length() < MIN_CIPHERTEXT_LENGTH)
		{
			throw new EncryptionException("Encrypted file is too short");
		}
		try
		{
			InputStream is = new BufferedInputStream(new FileInputStream(aCipherText));
			OutputStream os = new BufferedOutputStream(new FileOutputStream(aPlainText));
			byte[] header = new byte[PREFIX_LENGTH];
			is.read(header);
			byte[] lIV = Arrays.copyOfRange(header, 0, IV_LENGTH);
			byte[] lSalt = Arrays.copyOfRange(header, IV_LENGTH, PREFIX_LENGTH);
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec mSecretKeySpec = new SecretKeySpec(lSaltedPassword, SECRET_KEY_ALGORITHM);
			Cipher lCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			lCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, new IvParameterSpec(lIV));
			CipherOutputStream cos = new CipherOutputStream(os, lCipher);
			doCopy(is, cos);
		}
		catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			throw new IllegalStateException("Invalid encryption provider configuration", e);
		}
	}

	@SuppressWarnings("resource")
	public DataInputStream encryptFileToStream(File aPlainText) throws FileNotFoundException
	{
		try
		{
			InputStream is = new BufferedInputStream(new FileInputStream(aPlainText));
			
			byte[] lSalt = generateSalt();
			byte[] lSaltedPassword = getSaltedPassword(theKey, lSalt);
			SecretKeySpec lSecretKeySpec = new SecretKeySpec(lSaltedPassword, SECRET_KEY_ALGORITHM);
			Cipher lCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			lCipher.init(Cipher.ENCRYPT_MODE, lSecretKeySpec);
			byte[] lIV = lCipher.getIV();
			
			
			return
				new DataInputStream(
					new SequenceInputStream(
							new SequenceInputStream(
									new ByteArrayInputStream(lIV),
									new ByteArrayInputStream(lSalt)),
							new CipherInputStream(is, lCipher)));			
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) 
		{
			throw new IllegalStateException("Invalid encryption provider configuration", e);
		}		
	}
	
	private void doCopy(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[BUFFER_SIZE];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			os.write(bytes, 0, numBytes);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	public static long computeEncryptedLength(long plainTextlength)
	{
		return PREFIX_LENGTH + ((plainTextlength + BLOCK_SIZE) & ~(BLOCK_SIZE - 1));
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
	    	throw new IllegalStateException("Invalid encryption provider configuration", e);
		}
	}
}
