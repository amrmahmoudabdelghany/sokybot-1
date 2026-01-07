package org.sokybot.lockapiexample;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MultiLockExample {
	
	private static  Map<String , Lock>  locks = new HashMap<>() ; 
	 
	static { 
		
		locks.put("isro", new ReentrantLock()) ; 
		locks.put("vsro", new ReentrantLock()) ; 

		locks.put("ksro", new ReentrantLock()) ; 
		locks.put("rsro", new ReentrantLock()) ; 
		
	}
	
	
	
	private static Thread createThreadThatPass(String theParam) { 
		return new Thread(()->{
			update(theParam) ;
		});
	}
	public static void main(String args[]) { 
		 
		createThreadThatPass("isro").start();
		createThreadThatPass("isro").start();
		createThreadThatPass("isro").start();

		createThreadThatPass("vsro").start();

		createThreadThatPass("ksro").start();

		createThreadThatPass("rsro").start();

		createThreadThatPass("vsro").start();
		
	}
	
	private static void updateRepos(String param) { 
		
		try {
			System.out.println(param  + " updating repositories") ; 
			Thread.sleep(TimeUnit.SECONDS.toMillis(5));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void update(String param) { 
		Lock lock = locks.get(param) ; 
		try { 
			
			lock.lock(); 
			System.out.println("on update " + param) ; 
			updateRepos(param);
			
		}finally {
		   lock.unlock(); 
		}
		
		
	}
	

}
