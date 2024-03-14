package org.sokybot.pk2.exception;



import lombok.Getter;


/**
 * 
 * The pk2 file driver is working with assumption  that any pk2 file
 *     is encrypted - if encrypted - with constant key is {@code Constants.HEADER_VALIDATION_KEY}
 *     
 * Objects from this exception class represents the event when the pk2 driver is working with invalid key
 * @author Amr
 */
@Getter
public class Pk2InvalidKeyException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String targetFile ; 
	
	private byte[] usedKey ; 
	private byte[] verfication ; 
	
	public Pk2InvalidKeyException(String message ,String targetFile  , byte[] usedKey , byte[] verfication) {
		super(message);
		this.targetFile = targetFile ; 
		this.usedKey = usedKey  ; 
		this.verfication = verfication ; 
		
	}

	
	
}
