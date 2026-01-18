package org.sokybot.game.enums;


//ref - https://github.com/sokybot/SilkroadDoc/blob/master/Packets/GATEWAY/LoginErrorCode.cs
public enum LoginBlockType  {
	
	
	
	UNKNOWN(0) ,
	
    /// <summary>
    /// <para>UIO_MSG_ERROR_ACCOUNT_STOP</para>
    /// This account has been blocked according to the Terms of Service and cannot be accessed to play the game.
    /// Blocking reason:
    /// Completion time:
    /// </summary>
    Punishment(1),

    /// <summary>
    /// <para>UIO_MSG_ERROR_ACCOUNT_CONNECT_IMPOSSIBILE</para>
    /// Cannot connect to the server because the server is now in inspection.
    /// </summary>
    AccountInspection(2),

    /// <summary>
    /// <para>UIO_MSG_ERROR_THERE_IS_NO_ACCOUNT_INFO</para>
    /// ID is found, but the needed details are not found.\nFill in the needed information at Silkroad homepage to connect to the game.
    /// <para>SimpleMessageBox -> opens website</para>
    /// </summary>
    NoAccountInfo(3),

    /// <summary>
    /// <para>UIO_MSG_ERROR_GRATIS_USER_BLOCKED</para>
    /// Cannot connect because the free service is over.
    /// </summary>
    
    FreeServiceOver(4);

    private int code ; 
	
	private LoginBlockType(int val) {
		this.code = val ; 
	}
	
	public static LoginBlockType getBlockType(int code) { 
		for(LoginBlockType error : values()) { 
			if(error.code == code) return error ; 
		}
		
		return UNKNOWN ; 
		
	}
}
