package org.arquillian.smart.testing.mvn.ext.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.arquillian.smart.testing.configuration.Configuration;

/**
 * Resolves dependencies for strategies defined by keywords (e.g. new, changed, affected)
 * <p>
 * For example one can defined mapping between "awesome" strategy to its corresponding dependency as follows:
 * <p>
 * smart.testing.strategy.awesome=org.awesome:smart-testing-awesome:0.0.1
 * <p>
 * Version in the g:a:v can be omitted and will be resolved to the one matching "Smart Testing Extension" which is in use.
 * <p>
 * Properties are loaded with the following precedence:
 * <p>
 * - default ones stored internally are loaded first
 * - custom file can overwrite defaults
 * - System properties overwrite all above
 */
class StrategyDependencyResolver {

    protected static final String SMART_TESTING_STRATEGY_PREFIX = Configuration.SMART_TESTING_CUSTOM_STRATEGIES + ".";
    private String[] customStrategies;

    StrategyDependencyResolver() {
        this.customStrategies = new String[0];
    }

    StrategyDependencyResolver(String[] customStrategies) {
        this.customStrategies = customStrategies;
    }

    Map<String, Dependency> resolveDependencies() {
        final Properties properties = new Properties();
        properties.putAll(loadDefaultMapping());
        properties.putAll(loadCustomStrategies());
        return transformToDependencies(properties);
    }

    private Properties loadCustomStrategies() {
        final Properties properties = new Properties();

        final Map<String, String> collect = Arrays.stream(customStrategies)
            .collect(Collectors.toMap(def -> def.substring(0, def.indexOf('=')),
                def -> def.substring(def.indexOf('=') + 1), (value, value2) -> value2));
        properties.putAll(collect);
        return properties;
    }

    private Properties loadDefaultMapping() {
        final Properties properties = new Properties();
        try (InputStream strategyMapping = getClass().getClassLoader().getResourceAsStream("strategies.properties")) {
            if (strategyMapping == null) {
                throw new IllegalStateException("Unable to load default strategy dependencies mapping.");
            }
            properties.load(strategyMapping);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load default strategy dependencies mapping.", e);
        }
        return properties;
    }

    private Map<String, Dependency> transformToDependencies(Properties properties) {
        return properties
            .stringPropertyNames()
            .stream()
            .filter(key -> key.startsWith(SMART_TESTING_STRATEGY_PREFIX))
            .map(String::valueOf)
            .collect(Collectors.toMap(this::filterPrefix,
                key -> {
                    final String[] gav = ((String) properties.get(key)).split(":");
                    final Dependency dependency = new Dependency();
                    dependency.setGroupId(gav[0]);
                    dependency.setArtifactId(gav[1]);
                    String version = ExtensionVersion.version().toString();
                    if (gav.length == 3) {
                        version = gav[2];
                    }
                    dependency.setVersion(version);
                    dependency.setScope("runtime");
                    return dependency;
                }));
    }

    private String filterPrefix(String key) {
        return key.substring(SMART_TESTING_STRATEGY_PREFIX.length());
    }
}
