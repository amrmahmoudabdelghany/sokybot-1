package org.sokybot.lockapiexample;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceImp2 implements IService {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	private final Map<String, Integer> versionRegistry;

	public ServiceImp2(Map<String, Integer> versionRegistry) {
		this.versionRegistry = versionRegistry;
	}

	private boolean isNeedUpdate(String param) {
		log("isNeedUpdate", "Checking whether " + param + " need update or not");
		return (!this.versionRegistry.containsKey(param) || this.versionRegistry.get(param) < 3);
	}

	@Override
	public boolean checkVersion(String param) {
		try {
			log("checkVersion", "trying to acquire read lock when check version for " + param);
			this.readLock.lock();
			log("checkVersion", "acquired read lock when check version for " + param);
			boolean res = isNeedUpdate(param);
			if (res)
				log("checkVersion", "detecting that " + param + " need update");
			else
				log("checkVersion", param + " not need update");
			return !res;

		} finally {
			log("checkVersion", "trying to release read lock");
			this.readLock.unlock();
			log("checkVersion", "read lock has been released");
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

	private Set<String> lockedParams = Collections.synchronizedSet(new HashSet<>());

	@Override
	public void update(String param) {

		log("update", "trigger upate operation ");
		try {

			if (lockedParams.contains(param)) {
				this.writeLock.lock();
			}

			lockedParams.add(param) ; 
			this.writeLock.lock();
			
			if (isNeedUpdate(param)) {
				int newVersion = updateRepositories();

				this.versionRegistry.put(param, newVersion);

			} else
				log("update", param + " not need update"); // for test only

		} finally {
			if (lockedParams.contains(param)) {
				log("update", "trying to release write lock");
				this.writeLock.unlock();
				log("update", "write lock released");
			}
		}

	}

	public static void main(String args[]) {
		Map<String, Integer> registry = new HashMap<>();
		registry.put("isro", 1);
		registry.put("vsro", 2);

		IService service = new ServiceImp2(registry);

		Producer producer = new Producer(service, "isro");
		Producer producer2 = new Producer(service, "isro");
		RandomParameterProduer rnd = new RandomParameterProduer(service);

		producer.start();
		producer2.start();
		rnd.start();

	}

}
