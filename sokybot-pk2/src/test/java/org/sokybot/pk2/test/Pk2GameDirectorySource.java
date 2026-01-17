package org.sokybot.pk2.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.List;
import java.util.stream.Stream;

/**
 * JUnit 5 ArgumentsProvider for parameterized tests across multiple game directories.
 * Reads game directories from configuration and provides them as test arguments.
 * 
 * Usage:
 * <pre>
 * {@code @ParameterizedTest}
 * {@code @ArgumentsSource(Pk2GameDirectorySource.class)}
 * void testCompatibility(String gameDirectory, Pk2TestFixture fixture) {
 *     // Test logic
 * }
 * </pre>
 * 
 * @author sokybot
 */
public class Pk2GameDirectorySource implements ArgumentsProvider {
    
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        Pk2TestConfiguration config = Pk2TestConfiguration.getInstance();
        List<String> gameDirectories = config.getGameDirectories();
        
        if (gameDirectories.isEmpty()) {
            throw new IllegalStateException(
                "No game directories configured. Set system property 'pk2.test.game.paths', " +
                "environment variable 'PK2_TEST_GAME_PATHS', or create 'pk2-test-game-paths.txt' " +
                "in test resources.");
        }
        
        return gameDirectories.stream()
            .map(gameDir -> {
                Pk2TestFixture fixture = new Pk2TestFixture(gameDir);
                return Arguments.of(gameDir, fixture);
            });
    }
}