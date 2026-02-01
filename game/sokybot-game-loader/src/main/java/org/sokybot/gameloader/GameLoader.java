package org.sokybot.gameloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.osgi.service.component.annotations.Component;
import org.sokybot.loader.IGameLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game loader implementation.
 */
@Component(service = IGameLoader.class, immediate = true)
public class GameLoader implements IGameLoader {

	private static final Logger log = LoggerFactory.getLogger(GameLoader.class);
	private final IProcessLoader processLoader;

	public GameLoader() {
		this.processLoader = ProcessLoader.createInstance();
	}

	@Override
	public int launch(String clientPath) {
		// Default command arguments
		return launch(clientPath, "0 /22 0 0");
	}

	@Override
	public int launch(String clientPath, String command) {
		log.info("Launching client: {} with args: {}", clientPath, command);

		Path clientExecutable = Paths.get(clientPath);
		if (!Files.exists(clientExecutable) || !clientPath.toLowerCase().endsWith(".exe")) {
			throw new IllegalArgumentException("Invalid client path: " + clientPath);
		}

		try {
			ensureDependencies();

			String userDir = System.getProperty("user.dir");
			Path dllPath = Paths.get(userDir, "sokybotpatch.dll");
			Path shellPath = Paths.get(userDir, "shell.txt");

			return this.processLoader.loadProcessImage(
					clientPath,
					command,
					dllPath.toString(),
					shellPath.toString());
		} catch (IOException e) {
			log.error("Failed to prepare dependencies", e);
			throw new UncheckedIOException("Failed to launch client", e);
		}
	}

	private void ensureDependencies() throws IOException {
		String userDir = System.getProperty("user.dir");
		copyResource("sokybotpatch.dll", Paths.get(userDir, "sokybotpatch.dll"));
		copyResource("shell.txt", Paths.get(userDir, "shell.txt"));
	}

	private void copyResource(String resourceName, Path targetPath) throws IOException {
		if (!Files.exists(targetPath)) {
			try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
				if (is == null) {
					throw new IOException("Resource not found: " + resourceName);
				}
				Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
				log.debug("Copied resource {} to {}", resourceName, targetPath);
			}
		}
	}

	@Override
	public String getName() {
		return "Default";
	}

	@Override
	public Integer minimumVersion() {
		return 0;
	}

	@Override
	public Integer maximumVersion() {
		return 0;
	}
}
