package org.arquillian.smart.testing.configuration;

public class ConfigurationItem {

    private final String paramName;
    private String systemProperty;
    private Object defaultValue;

    public ConfigurationItem(String paramName, String systemProperty, Object defaultValue) {
        this.paramName = paramName;
        this.systemProperty = systemProperty;
        this.defaultValue = defaultValue;
    }

    public ConfigurationItem(String paramName, String systemProperty) {
        this.paramName = paramName;
        this.systemProperty = systemProperty;
    }

    public ConfigurationItem(String paramName) {
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }

    public String getSystemProperty() {
        return systemProperty;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
