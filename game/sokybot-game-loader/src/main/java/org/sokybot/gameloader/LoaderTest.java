package org.sokybot.gameloader;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class LoaderTest {

	public static void main(String args[]) {
		
 
		
		//String sro_client_path = "E:\\Amroo\\Silkroad Games\\Silkroad\\sro_client.exe"  ; 
	      String sro_client_path = "E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019\\sro_client.exe" ; 
		String shell_path = "C:\\Users\\root\\Desktop\\TEST2\\shell.txt" ; 
		String dll_path =    "C:\\Users\\root\\Desktop\\TEST2\\Patch2.dll" ; 
		
		
		ProcessLoader processLoader = new ProcessLoader() ;
		processLoader.loadProcessImage(sro_client_path, 
	   			"0 /22 0 0" ,  dll_path, shell_path); 
		
		
		try {
			ServerSocket socket = new ServerSocket(15000) ;
			Socket clientSocket = socket.accept() ; 
			System.out.println("Client redirected successfully") ; 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}

}
