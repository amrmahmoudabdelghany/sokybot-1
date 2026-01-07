package org.sokybot.lockapiexample;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceImpl3 implements IService {

	private Map<String, Integer> versionRegistry;
	private final Map<String, Lock> lockCash = Collections.synchronizedMap(new HashMap<>());

	public ServiceImpl3(Map<String, Integer> versionRegistry) {
		this.versionRegistry = versionRegistry;
	}

	private void lock(String param) {

		if (!lockCash.containsKey(param)) {
			lockCash.put(param, new ReentrantLock());
		}

		this.lockCash.get(param).lock();
	}

	private void unlock(String param) {
		if (this.lockCash.containsKey(param)) {
			this.lockCash.get(param).unlock();
		}
	}

	@Override
	public boolean checkVersion(String param) {

		try { 
			log("checkVersion" , "trigger checkVersion operation") ; 
			lock(param) ; 
			log("checkVersion" , "acquired lock ") ; 
			
			boolean res = isNeedUpdate(param);
	
			if (res)
				log("checkVersion", "detecting that " + param + " need update");
			else
				log("checkVersion", param + " not need update");
			return !res;
	
		}finally { 
			log("checkVersion", "trying to release" +param+ " lock");
			unlock(param) ; 
			log("checkVersion" , param + " lock has been released") ; 
		}
	
	}

	@Override
	public void update(String param) {

		log("update", "trigger upate operation ");

		try {

			lock(param);

			if (isNeedUpdate(param)) {
				int newVersion = updateRepositories();

				this.versionRegistry.put(param, newVersion);

			} else
				log("update", param + " not need update"); // for test only

		} finally {
			log("update", "trying to release" +param+ " lock");
			unlock(param) ; 
			log("update" , param + " lock has been released") ; 
		}

	}

	private int updateRepositories() {
		log("updateRepositories", "updating repositories");
		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(5));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log("updateRepositories", "updating finish");
		return 3;
	}

	private boolean isNeedUpdate(String param) {

		// in actual implementation this method must extract current version from
		// media.pk2
		// and check whether registered version is valid or not
		// registered version is valid if equal to current version

		log("isNeedUpdate", "Checking whether " + param + " need update or not");
		return (!this.versionRegistry.containsKey(param) || this.versionRegistry.get(param) < 3);
	}

	public static void main(String args[]) {
		Map<String, Integer> registry = new HashMap<>();

		registry.put("isro", 1);
		registry.put("vsro", 2);

		IService service = new ServiceImpl3(registry);

		Producer producer = new Producer(service, "isro");
		Producer producer2 = new Producer(service, "isro");
		RandomParameterProduer rnd = new RandomParameterProduer(service);

		producer.start();
		producer2.start();
		rnd.start();

	}

	private String getName() {
		return Thread.currentThread().getName();
	}

	private String prefix() {

		Calendar calendar = Calendar.getInstance();

		return calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ":"
				+ calendar.get(Calendar.MILLISECOND) + " ( " + getName() + " ) : ";
	}

	private void log(String methodName, String message) {
		System.out.println(prefix() + " " + methodName + " -- " + message);
	}

}
