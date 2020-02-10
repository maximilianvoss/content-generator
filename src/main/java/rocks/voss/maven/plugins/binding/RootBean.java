package rocks.voss.maven.plugins.binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Data
public class RootBean {
    List<rocks.voss.maven.plugins.binding.ConfigurationBean> configurations;

    public static RootBean createBeanByFile(String path) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        try {
            return mapper.readValue(new File(path), RootBean.class);
        } catch (IOException e) {
            System.err.println(e);
        }
        return null;
    }
}
