package rocks.voss.maven.plugins.binding;

import lombok.Data;

import java.util.Map;

@Data
public class ConfigurationBean {
    private String name;
    private Map<String, String> keys;
}
