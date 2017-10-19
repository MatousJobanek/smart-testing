package org.arquillian.smart.testing.mvn.ext.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class DefaultStrategyMappingLoader {

    Properties load() {
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
}
