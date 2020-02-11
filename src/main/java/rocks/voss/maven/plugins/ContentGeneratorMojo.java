package rocks.voss.maven.plugins;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import rocks.voss.maven.plugins.binding.ConfigurationBean;
import rocks.voss.maven.plugins.binding.RootBean;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ContentGeneratorMojo extends AbstractMojo {

    @Parameter(required = true, readonly = true)
    private String config;

    @Parameter(required = true, readonly = true)
    private String template;

    @Parameter(required = true, readonly = true)
    private String target;


    public void execute() throws MojoExecutionException {
        RootBean rootBean = RootBean.createBeanByFile(config);
        if (rootBean == null) {
            throw new IllegalStateException("Was not able to load YAML file from: " + config);
        }
        for (ConfigurationBean environment : rootBean.getConfigurations()) {
            parseDirectory(environment, new File(template));
        }
    }

    private void parseDirectory(ConfigurationBean set, File file) {
        Arrays.stream(file.listFiles()).forEach(child -> {
            String pathIn = getRelativePath(child);
            String pathOut = target + replaceVariables(set, pathIn);

            if (child.isDirectory()) {
                createDirectory(pathOut);
                parseDirectory(set, child);
            } else if (child.isFile()) {
                File fileOut = new File(pathOut);
                try {
                    Files.copy(child.toPath(), fileOut.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    updateFileContent(set, fileOut);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to copy file from: " + child.getAbsolutePath() + " to: " + fileOut.getAbsolutePath(), e);
                }
            } else {
                throw new IllegalStateException("Unable to handle: " + child.getAbsolutePath());
            }
        });
    }

    private String getRelativePath(File file) {
        String filePath = file.getAbsolutePath();
        File blueprintFile = new File(template);
        String blueprintPath = blueprintFile.getAbsolutePath();
        String relativePath = StringUtils.removeStart(filePath, blueprintPath);
        return relativePath;
    }

    private void createDirectory(String path) {
        File file = new File(path);
        file.mkdirs();
    }

    private void updateFileContent(ConfigurationBean set, File file) {
        try {
            BufferedReader reader = new BufferedReader((new FileReader(file)));
            StringBuilder stringBuilder = new StringBuilder();
            while (reader.ready()) {
                stringBuilder.append(reader.readLine());
                stringBuilder.append("\n");
            }
            reader.close();

            String inputData = stringBuilder.toString();
            String outputData = replaceVariables(set, inputData);

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(outputData);
            writer.close();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new IllegalStateException("IO Exception on: " + file.getAbsolutePath(), e);
        }
    }

    private String replaceVariables(ConfigurationBean set, String input) {
        Pattern pattern = Pattern.compile("%\\(([\\w|-]+)\\)");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (set.getKeys().containsKey(match)) {
                input = StringUtils.replace(input, "%(" + match + ")", set.getKeys().get(match));
                continue;
            } else {
                throw new IllegalStateException("Variable: " + match + " not defined!");
            }
        }
        return input;
    }
}
