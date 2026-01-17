package org.sokybot.pk2.test;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * JUnit 5 extension for automatic Pk2TestFixture injection.
 * 
 * Usage:
 * <pre>
 * {@code @ExtendWith(Pk2TestExtension.class)}
 * class MyTest {
 *     {@code @InjectPk2Fixture}
 *     private Pk2TestFixture fixture;
 *     
 *     {@code @Test}
 *     void test() {
 *         // Use fixture
 *     }
 * }
 * </pre>
 * 
 * @author sokybot
 */
public class Pk2TestExtension implements BeforeEachCallback, ParameterResolver {
    
    private static final Namespace NAMESPACE = Namespace.create(Pk2TestExtension.class);
    private static final String FIXTURE_KEY = "pk2TestFixture";
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        
        // Inject fixture into fields annotated with @InjectPk2Fixture
        Class<?> testClass = context.getRequiredTestClass();
        while (testClass != null) {
            for (Field field : testClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(InjectPk2Fixture.class) &&
                    field.getType() == Pk2TestFixture.class) {
                    field.setAccessible(true);
                    Pk2TestFixture fixture = getOrCreateFixture(context);
                    field.set(testInstance, fixture);
                }
            }
            testClass = testClass.getSuperclass();
        }
    }
    
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == Pk2TestFixture.class &&
               parameterContext.getParameter().isAnnotationPresent(InjectPk2Fixture.class);
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getOrCreateFixture(extensionContext);
    }
    
    private Pk2TestFixture getOrCreateFixture(ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);
        return store.getOrComputeIfAbsent(FIXTURE_KEY, key -> {
            Pk2TestConfiguration config = Pk2TestConfiguration.getInstance();
            if (!config.isConfigured()) {
                throw new Pk2TestConfiguration.Pk2TestConfigurationException(
                    "No game directory configured for Pk2TestFixture injection. " +
                    "Set system property 'pk2.test.game.path' or environment variable 'PK2_TEST_GAME_PATH'.");
            }
            return new Pk2TestFixture();
        }, Pk2TestFixture.class);
    }
    
    /**
     * Annotation to mark fields or parameters for Pk2TestFixture injection.
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface InjectPk2Fixture {
    }
}