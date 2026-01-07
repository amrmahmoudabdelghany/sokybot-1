package org.sokybot.lockapiexample;

import java.util.Random;

public class RandomParameterProduer extends Thread{
	

	private IService service;

	public RandomParameterProduer(IService service) {
		this.service = service;
		setName("RandomParam-Thread");
	}

	@Override
	public void run() {

		String rndParam = Integer.toString((new Random()).nextInt());

		if (!service.checkVersion(rndParam)) {

			service.update(rndParam);
		}

	}

}
