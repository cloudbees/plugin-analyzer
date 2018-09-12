package com.cloudbees.pluginanalyzer;

import com.cloudbees.jenkins.plugins.updates.envelope.EnvelopePlugin;
import com.cloudbees.jenkins.plugins.updates.envelope.EnvelopeProduct;
import com.cloudbees.jenkins.plugins.updates.envelope.ParsedEnvelope;
import com.cloudbees.jenkins.plugins.updates.envelope.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Stream;

/**
 * <p>PluginAnalyzer class.</p>
 *
 * @author Mikael Gaunin
 * @since 0.1.0
 */
public class PluginAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAnalyzer.class);

    public static final String REPO_URL = "https://nexus-internal.cloudbees.com/content/repositories/releases/";
    private static final String ENVELOPE_CLASSIFIER = "envelope";
    private static final String ENVELOPE_EXTENSION = "json";

    private static final char URL_SEPARATOR = '/';
    private static final char FILENAME_SEPARATOR = '-';
    private static final char FILE_SEPARATOR = '.';

    protected static final File TARGET_FILE = new File(System.getProperty("user.dir")
                                                                + System.getProperty("file.separator")
                                                                + "target");
    protected static final String CSV_FILE_NAME = TARGET_FILE.getAbsolutePath()
                                                + System.getProperty("file.separator")
                                                + "Analyzed-Plugins.csv";

    private String pluginListFilePath;
    private EnvelopeProduct product;
    private String productRelease;

    public PluginAnalyzer(String pluginListFilePath, String productId, String productRelease)
    throws IOException {
        if (pluginListFilePath == null || !new File(pluginListFilePath).exists()) {
                throw new IOException("File not found: " + pluginListFilePath);
        }
        this.pluginListFilePath = pluginListFilePath;
        this.product = EnvelopeProduct.valueOf(productId.toUpperCase());
        this.productRelease = productRelease;
        if (!TARGET_FILE.exists() && !TARGET_FILE.mkdir()) {
            LOGGER.warn("Something went wrong with directory creation: {}", TARGET_FILE.getAbsolutePath());
        }
    }

    private String getPluginListFilePath() {
        return pluginListFilePath;
    }

    public static void main(String[] args) {
        if (args.length == 5) {
            try {
                LOGGER.info("Processing...");
                LOGGER.info("NB: password with specific characters must be written between ''");
                new PluginAnalyzer(args[0], args[1], args[2]).proceedPluginsAnalysis(args[3], args[4]);
                LOGGER.info("That's it!");
            } catch (Exception e) {
                LOGGER.error("Process failed!", e);
                System.exit(1);
            }
        } else {
            LOGGER.error("Argument must be: pluginListFilePath productId productRelease userName password");
            System.exit(1);
        }
    }

    public String getEnvelopeFileName() {
        StringBuilder fileName = new StringBuilder((product.getArtifactId()));
        fileName.append(FILENAME_SEPARATOR);
        fileName.append(productRelease);
        fileName.append(FILENAME_SEPARATOR);
        fileName.append(ENVELOPE_CLASSIFIER);
        fileName.append(FILE_SEPARATOR);
        fileName.append(ENVELOPE_EXTENSION);
        return fileName.toString();
    }

    /**
     * URL ex: https://nexus-internal.cloudbees.com/content/repositories/releases/
     * com/cloudbees/operations-center/server/operations-center-war/2.121.3.1/
     * operations-center-war-2.121.3.1-envelope.json
     *
     * @return
     * @throws Exception
     */
    public URL getEnvelopeUrl()
        throws MalformedURLException {
        return getEnvelopeUrlFromRepo(REPO_URL);
    }

    public URL getEnvelopeUrlFromRepo(final String repo_url)
        throws MalformedURLException {
        StringBuilder url = new StringBuilder(repo_url);
        url.append(product.getGroupId().replace('.', URL_SEPARATOR));
        url.append(URL_SEPARATOR);
        url.append(product.getArtifactId());
        url.append(URL_SEPARATOR);
        url.append(productRelease);
        url.append(URL_SEPARATOR);
        url.append(getEnvelopeFileName());
        return new URL(url.toString());
    }

    /**
     *
     * @throws Exception
     */
    private void proceedPluginsAnalysis(final String userName, final String password)
    throws Exception {
        List<List> lines = this.analyzePlugins( this.getFilePlugins(),
                                                this.getEnvelopePlugins(userName, password));
        this.generateCsvFile(lines);
    }

    /**
     *
     * @return Map<String, String>: Plugins
     * @throws IOException
     */
    protected Map<String, String> getFilePlugins()
    throws IOException {
        HashMap<String, String> pluginsMap = new HashMap<>();
        Stream<String> pluginsStream = null;
        try {
            pluginsStream = Files.lines(Paths.get(getPluginListFilePath()));
            pluginsStream.forEach(plugin -> {   StringTokenizer st = new StringTokenizer(plugin, ":");
                                                pluginsMap.put(st.nextToken(), st.nextToken());});
        } finally {
            if (pluginsStream != null) {
                pluginsStream.close();
            }
        }
        return pluginsMap;
    }

    /**
     *
     * @return Map<String, EnvelopePlugin>: Envelope plugins
     * @throws Exception
     */
    private Map<String, EnvelopePlugin> getEnvelopePlugins(final String userName, final String password)
        throws Exception {
        return getEnvelopePlugins(REPO_URL, false, userName, password);
    }

    protected Map<String, EnvelopePlugin> getEnvelopePlugins(final String repo_url)
        throws Exception {
        return getEnvelopePlugins(repo_url, true, null, null);
    }

    private Map<String, EnvelopePlugin> getEnvelopePlugins(final String repo_url, final boolean isFileProtocol,
                                                           final String userName, final String password)
        throws Exception {
        URL url = getEnvelopeUrlFromRepo(repo_url);
        InputStream inputStream;
        if (isFileProtocol) {
            inputStream = url.openStream();
        } else {
            final String envelopeFileName = getEnvelopeFileName();
            final File envelopeFile = new File(TARGET_FILE, envelopeFileName);
            List<String> commandLine = new ArrayList<>();
            commandLine.add("wget");
            commandLine.add("--user");
            commandLine.add(userName);
            commandLine.add("--password");
            commandLine.add(password);
            commandLine.add("-O");
            commandLine.add(envelopeFile.getAbsolutePath());
            commandLine.add("-S");
            commandLine.add(url.toString());
            if (!envelopeFile.exists()) {
                Process p = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
                p.waitFor();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(),
                                                                Charset.forName("UTF-8")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LOGGER.error(line);
                    }
                }
            }
            inputStream = new FileInputStream(envelopeFile);
        }
        Validation<ParsedEnvelope> validation = ParsedEnvelope.loader().fromJSON(inputStream);
        inputStream.close();
        return validation.get().getEnvelope().getPlugins();
    }

    /**
     *
     * @param plugins
     * @param envelopePlugins
     * @throws Exception
     */
    protected List<List> analyzePlugins(Map<String, String> plugins, Map<String, EnvelopePlugin> envelopePlugins) {
        List<List> lines = new ArrayList<>();
        lines.add(Arrays.asList("Id",
                                "Name",
                                "Version",
                                "Envelope",
                                "Version",
                                "Type",
                                "Scope"));
        for (Map.Entry<String, String> plugin : plugins.entrySet()) {
            if (envelopePlugins.get(plugin.getKey()) == null) {
                lines.add(Arrays.asList(plugin.getKey(),
                                        "",
                                        plugin.getValue(),
                                        "NO",
                                        "",
                                        "",
                                        ""));
            } else {
                lines.add(Arrays.asList(plugin.getKey(),
                                        envelopePlugins.get(plugin.getKey()).getName(),
                                        plugin.getValue(),
                                        "YES",
                                        envelopePlugins.get(plugin.getKey()).getVersionNumber().toString(),
                                        envelopePlugins.get(plugin.getKey()).getTier().toString(),
                                        envelopePlugins.get(plugin.getKey()).getScope().toString()));
            }
        }
        return lines;
    }

    /**
     *
     * @param lines
     * @throws IOException
     */
    protected void generateCsvFile(List<List> lines)
    throws IOException {
        StringBuilder content = new StringBuilder();
        for (List line : lines) {
            content.append(String.join(",", line));
            content.append("\n");
        }
        File csvFile = new File(CSV_FILE_NAME);
        if (csvFile.exists()) {
            Files.delete(csvFile.toPath());
        }
        if (csvFile.createNewFile()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CSV_FILE_NAME),
                                                                    Charset.forName("UTF-8"))) {
                writer.write(content.toString());
            }
        } else {
            throw new IOException(String.format("Cannot create the target file: %s", CSV_FILE_NAME));
        }
    }
}
