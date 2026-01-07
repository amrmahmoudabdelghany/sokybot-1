package org.sokybot.lockapiexample;

public class Producer extends Thread {

	
	private IService service;
	private String staticParam;
	private static int counter = 0;

	public Producer(IService service, String staticParam) {
		this.service = service;
		this.staticParam = staticParam;
		setName("Producer-" + counter + "-Thread");
		counter++;
	}

	@Override
	public void run() {

		if (!service.checkVersion("isro")) {
			service.update("isro");
		} else {
			System.out.println(getName() + " skip update operation ");
		}
	}

	
}
