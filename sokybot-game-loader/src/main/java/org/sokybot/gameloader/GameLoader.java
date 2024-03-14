package org.sokybot.gameloader;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.sokybot.service.IGameLoader;

//@Component(service = IGameLoader.class  , immediate = true  )
public class GameLoader implements IGameLoader, BundleActivator {

	private IProcessLoader processLoader;

	public GameLoader() {

		this.processLoader = ProcessLoader.createInstance();
	}

	@Override
	public int launch(String clientPath) {

		return launch(clientPath, "0 /" + 22 + " 0 0");

	}

	@Override
	public int launch(String clientPath, String command) {

		if (!Files.exists(Paths.get(clientPath)) || !clientPath.endsWith(".exe")) {
			throw new IllegalArgumentException("Invalid client path");
		}

		String dllRef = System.getProperty("user.dir").concat("\\").concat("sokybotpatch.dll");

		Path dllPath = Paths.get(dllRef);

		if (!Files.exists(dllPath)) {
			try {
				Files.copy(getClass().getClassLoader().getResourceAsStream("sokybotpatch.dll"), dllPath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		String shellRef = System.getProperty("user.dir").concat("\\").concat("shell.txt");
		Path shellPath = Paths.get(shellRef);

		if (!Files.exists(shellPath)) {
			try {
				Files.copy(getClass().getClassLoader().getResourceAsStream("shell.txt"), shellPath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return this.processLoader.loadProcessImage(clientPath, command, dllRef, shellRef);

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Default";
	}

	@Override
	public Integer minimumVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Integer maximumVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void start(BundleContext context) throws Exception {

		System.out.println("On Registering Service");
		context.registerService(IGameLoader.class, this, null);

	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}
}
