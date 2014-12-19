// CS6813 Final project
// Author: Andriy Goltsev
// Date: 12/19/2014

package edu.poly.cs6813.andriygoltsev.finalproject;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class MyCipher {

	private static final int KEY_LENGTH = 16;
	private static final String ALGO = "AES";
	private static final String DELIMITER  = "\t";
	private static final List<String> decrypedLines = new LinkedList<String>();
	private static final String help = "Very simple encryptor/decryptor that uses AES \n"
										+ "java -jar cipher.jar encrypt <input file> <output file>\n"
										+ "java -jar cipher.jar decrypt <encrypted file>\n"
										+ "java -jar cipher.jar demo demo\n"
										+ "java -jar cipher.jar\n"
										+ "Note, for tab delimited text file only the first column will be encrypted.";

	// http://www.code2learn.com/2011/06/encryption-and-decryption-of-data-using.html
	
	public static String encrypt(String Data, String passphrase)
			throws Exception {
		Key key = generateKey(passphrase);
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encVal = c.doFinal(Data.getBytes());
		String encryptedValue = Base64.getEncoder().encodeToString(encVal);
		return encryptedValue;
	}

	public static String decrypt(String encryptedData, String passphrase)
			throws Exception {
		Key key = generateKey(passphrase);
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		byte[] decordedValue = Base64.getDecoder().decode(encryptedData); // new
																			// BASE64Decoder().decodeBuffer(encryptedData);
		byte[] decValue = c.doFinal(decordedValue);
		String decryptedValue = new String(decValue);
		return decryptedValue;
	}

	private static Key generateKey(String passphrase) throws Exception {
		passphrase = passphrase.trim();
		if (passphrase.length() > KEY_LENGTH) {
			passphrase = passphrase.substring(0, KEY_LENGTH);
		} else if (passphrase.length() < KEY_LENGTH) {
			StringBuilder sb = new StringBuilder(passphrase);
			while (sb.length() < KEY_LENGTH) {
				sb.append('x');
			}
			passphrase = sb.toString();
		}

		Key key = new SecretKeySpec(passphrase.getBytes(), ALGO);
		return key;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println(help);
		}
		else if("DEMO".equalsIgnoreCase(args[0])) {
			runDemoMode();
		} else if ("ENCRYPT".equalsIgnoreCase(args[0])) {
			writeToFile(args.length == 3 ? args[2] : null, encryptFile(args[1]));
		} else if ("DECRYPT".equalsIgnoreCase(args[0])) {
			decryptFile(args[1]);
		}
	}
	
	private static void writeToFile(String fname, List<String> list) {
		BufferedWriter bufferedWriter = null;
		if(fname == null || fname.isEmpty()){
			fname = "out.encryped";
		}
		
		try {
		    bufferedWriter = new BufferedWriter(new FileWriter(fname));
		    for(String s : list) {
		        bufferedWriter.write(s);
		        // write a new line
		        bufferedWriter.newLine();
		        // flush
		        bufferedWriter.flush();
		    }
		} catch (IOException e) {
			System.err.println("Cannot write to file " + fname);
		} 
		
		if (bufferedWriter != null) {
			try {
				bufferedWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private static List<String> encryptFile(String fname) {
		
		Console console = System.console();
		final String password = String.valueOf(console.readPassword("[%s]", "Enter Password:"));
		List<String> processed  = new LinkedList<String>();
		
		Optional.of(readFile(fname)).ifPresent(
				lines -> lines.forEach(listList -> {
					String encryped;
					try {
						encryped = encrypt(listList.get(0), password);
						listList.set(0, encryped);
						processed.add(listList.stream().reduce(null, (a, e) -> a == null ? e : a + DELIMITER + e));
					} catch (Exception e) {
						System.err.println("Encrypting " + listList.get(0)
								+ " failed");
					}
				}));
		
		return processed;
		
	}
	
	private static List<List<String>> readFile(String fname) {
		Stream<String> lines = null;
		List<List<String>> listLines = null;
		try {
			lines = Files.lines(Paths.get(fname));
			listLines = lines.filter(line -> line != null && !line.isEmpty())
					.map(line -> Arrays.asList(line.split(DELIMITER)))
					.collect(Collectors.toList());
		} catch (IOException e) {
			System.err.println("Could not read file " + fname);
		} catch (Exception e) {
			System.err.println("Could not process file " + fname);
		}
		
		finally {
			if (lines != null) {
				lines.close();
			}
		}
		
		return listLines;
	}
	
	private static void decryptFile(String fname) {
		decrypedLines.clear();
		Console console = System.console();
		final String password = String.valueOf(console.readPassword("[%s]", "Enter Password:"));
		
		Optional.of(readFile(fname)).ifPresent(
				lines -> lines.forEach(listList -> {
					String decryped;
					try {
						decryped = decrypt(listList.get(0), password);
						listList.set(0, decryped);
						decrypedLines.add(listList.stream().reduce(null, (a, e) -> a == null ? e : a + DELIMITER + e));
					} catch (Exception e) {
						System.err.println("Decrypting " + listList.get(0)
								+ " failed");
					}
				}));
		
		System.out.println();
		if("p".equals(console.readLine("Enter 'p' to print result "))) {
			decrypedLines.forEach(System.out::println);
		};
		
	}

	private static void runDemoMode() throws Exception {
		Console console = System.console();
		String secret, encrypted, decrypted;
		char[] password;

		while (true) {
			System.out.println("Enter something to encrypt:");
			secret = console.readLine();
			password = console.readPassword("[%s]", "Password:");
			encrypted = encrypt(secret, String.valueOf(password));
			System.out.println(encrypted);

			System.out.println("Lets try to decrypt");
			password = console.readPassword("[%s]", "Password:");
			decrypted = decrypt(encrypted, String.valueOf(password));
			System.out.println(decrypted);
		}
	}
}
