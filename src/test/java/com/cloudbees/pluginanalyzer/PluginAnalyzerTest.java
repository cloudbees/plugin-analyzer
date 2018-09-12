package com.cloudbees.pluginanalyzer;

import com.cloudbees.jenkins.plugins.updates.envelope.EnvelopePlugin;
import com.cloudbees.jenkins.plugins.updates.envelope.PluginTier;
import com.cloudbees.jenkins.plugins.updates.envelope.Scope;
import com.google.common.collect.ImmutableSortedMap;
import hudson.util.VersionNumber;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginAnalyzerTest {

    private PluginAnalyzer pluginAnalyzer;
    private Map<String, String> filePlugins;
    private File pluginsFile;
    private File repo;
    private File envelopeFile;
    private Map<String, EnvelopePlugin> envelopePlugins;
    private List<List> lines;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void initialization() {
        try {
            pluginsFile = folder.newFile("active.txt");
            repo = folder.newFolder( ".m2");
            File targetFolder = new File (repo, "com/cloudbees/jenkins/main/jenkins-enterprise-war/2.107.3.4");
            if (!targetFolder.mkdirs()) {
                throw new IOException("a file with the name '" + targetFolder + "' already exists in the test folder");
            }
            envelopeFile = new File(targetFolder, "jenkins-enterprise-war-2.107.3.4-envelope.json");
            if (!envelopeFile.createNewFile()) {
                throw new IOException("a file with the name '" + envelopeFile + "' already exists in the test folder");
            }
            pluginAnalyzer = new PluginAnalyzer(pluginsFile.getAbsolutePath(), "cje", "2.107.3.4");

        } catch (IOException e) {
            e.printStackTrace();
            assertTrue("Initialization error", false);
        }
        buildFilePlugins();
        buildEnvelopePlugins();
        buildLines();
    }

    @Test
    public void testGetEnvelopeUrl() {
        final String expectedOC = "https://nexus-internal.cloudbees.com/content/repositories/releases/"
            + "com/cloudbees/operations-center/server/operations-center-war/2.107.3.4/"
            + "operations-center-war-2.107.3.4-envelope.json";
        final String expectedMaster = "https://nexus-internal.cloudbees.com/content/repositories/releases/"
            + "com/cloudbees/jenkins/main/jenkins-enterprise-war/2.107.3.4/"
            + "jenkins-enterprise-war-2.107.3.4-envelope.json";
        try {
            final PluginAnalyzer paCjoc = new PluginAnalyzer(pluginsFile.getAbsolutePath(),
                                                            "cjoc", "2.107.3.4");
            assertEquals(expectedOC, paCjoc.getEnvelopeUrl().toString());
            assertEquals(expectedMaster, pluginAnalyzer.getEnvelopeUrl().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testLoadingPluginsFromFile() {
        try {
            // Plugins file initialization
            assertTrue(pluginsFile.exists());
            BufferedWriter writer = new BufferedWriter(new FileWriter(pluginsFile.getAbsolutePath()));
            writer.write(fileContent);
            writer.close();
            assertEquals(filePlugins,
                         pluginAnalyzer.getFilePlugins());
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue("testLoadingPluginsFromFile", false);
        }
    }

    @Test
    public void testLoadingPluginsFromEnvelope() {
        try {
            // Plugins file initialization
            assertTrue(envelopeFile.exists());
            BufferedWriter writer = new BufferedWriter(new FileWriter(envelopeFile.getAbsolutePath()));
            writer.write(envelopeContent);
            writer.close();
            String path = "file:" + repo.getAbsolutePath() + "/";
            assertEquals(ImmutableSortedMap.copyOf(envelopePlugins).toString(),
                         ImmutableSortedMap.copyOf(pluginAnalyzer.getEnvelopePlugins(path)).toString());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("testLoadingPluginsFromEnvelope", false);
        }
    }

    @Test
    public void testGetPluginsInformation() {
        assertEquals(lines.toString(),
                     pluginAnalyzer.analyzePlugins(filePlugins, envelopePlugins).toString());
    }

    @Test
    public void testCsvFileCreation() {
        try {
            pluginAnalyzer.generateCsvFile(lines);
            String content = new String(Files.readAllBytes(Paths.get(PluginAnalyzer.TARGET_FILE.getAbsolutePath(),
                                                                "Analyzed-Plugins.csv")));
            assertEquals(csvFileContent, content);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue("testCsvFileCreation", false);
        }
    }

    // Keep it like this to debug accessing to remote repository
    // @Test
    public void testMain() {
        List<String> args = new ArrayList<>();
        args.add("src/test/resources/active.txt");
        args.add("cje");
        args.add("2.89.4.2");
        args.add("username");
        args.add("password");
        PluginAnalyzer.main(args.toArray(new String[0]));
    }

    private void buildFilePlugins() {
        filePlugins = new HashMap<>();
        filePlugins.put("active-directory", "2.4");
        filePlugins.put("ant", "1.4");
        filePlugins.put("apache-httpcomponents-client-4-api", "4.5.3-2.0");
        filePlugins.put("async-http-client", "1.7.24.1");
    }

    private void buildEnvelopePlugins() {
        envelopePlugins = new HashMap<>();
        HashMap<String, VersionNumber> dependencies = new HashMap<>();
        HashMap<String, VersionNumber> optionalDependencies = new HashMap<>();
        HashMap<String, VersionNumber> otherDependencies = new HashMap<>();
        envelopePlugins.put("async-http-client", new EnvelopePlugin("async-http-client",
                                                                    "Async Http Client",
                                                                    "org.jenkins-ci.plugins",
                                                                    "async-http-client",
                                                                     new VersionNumber("1.7.24.1"),
                                                                     dependencies,
                                                                     optionalDependencies,
                                                                     otherDependencies,
                                                                     Scope.BOOTSTRAP,
                                                                    "SC/TzSN+eOrewaDZJZyzLpIQV7E=",
                                                                     PluginTier.VERIFIED));
        dependencies = new HashMap<>();
        optionalDependencies = new HashMap<>();
        otherDependencies = new HashMap<>();
        envelopePlugins.put("structs", new EnvelopePlugin(  "structs",
                                                            "Structs Plugin",
                                                            "org.jenkins-ci.plugins",
                                                            "structs",
                                                             new VersionNumber("1.14"),
                                                             dependencies,
                                                             optionalDependencies,
                                                             otherDependencies,
                                                             Scope.BOOTSTRAP,
                                                            "yQrTUhP5jDuBYEyVqyw06Zy9TqA=",
                                                             PluginTier.VERIFIED));
        dependencies = new HashMap<>();
        optionalDependencies = new HashMap<>();
        otherDependencies = new HashMap<>();
        dependencies.put("structs", new VersionNumber("1.6"));
        envelopePlugins.put("ant", new EnvelopePlugin(  "ant",
                                                        "Ant Plugin",
                                                        "org.jenkins-ci.plugins",
                                                        "ant",
                                                         new VersionNumber("1.8"),
                                                         dependencies,
                                                         optionalDependencies,
                                                         otherDependencies,
                                                         Scope.FAT,
                                                        "xwEeOLr2K9b3Vu5TvSI1jQYpMjE=",
                                                         PluginTier.VERIFIED));
        dependencies = new HashMap<>();
        optionalDependencies = new HashMap<>();
        otherDependencies = new HashMap<>();
        envelopePlugins.put("durable-task", new EnvelopePlugin( "durable-task",
                                                                "Durable Task Plugin",
                                                                "org.jenkins-ci.plugins",
                                                                "durable-task",
                                                                 new VersionNumber("1.22"),
                                                                 dependencies,
                                                                 optionalDependencies,
                                                                 otherDependencies,
                                                                 Scope.FAT,
                                                                "CAGRX6lT6zBhwr5XOsYAoPCc7kY=",
                                                                 PluginTier.COMPATIBLE));

    }

    private void buildLines() {
        lines = new ArrayList<>();
        lines.add(Arrays.asList("Id",
                                "Name",
                                "Version",
                                "Envelope",
                                "Version",
                                "Type",
                                "Scope"));
        lines.add(Arrays.asList("async-http-client",
                                "Async Http Client",
                                "1.7.24.1",
                                "YES",
                                "1.7.24.1",
                                PluginTier.VERIFIED.toString(),
                                Scope.BOOTSTRAP.toString()));
        lines.add(Arrays.asList("apache-httpcomponents-client-4-api",
                                "",
                                "4.5.3-2.0",
                                "NO",
                                "",
                                "",
                                ""));
        lines.add(Arrays.asList("ant",
                                "Ant Plugin",
                                "1.4",
                                "YES",
                                "1.8",
                                PluginTier.VERIFIED.toString(),
                                Scope.FAT.toString()));
        lines.add(Arrays.asList("active-directory",
                                "",
                                "2.4",
                                "NO",
                                "",
                                "",
                                ""));
    }

    final private String fileContent = "active-directory:2.4:not-pinned\n" +
        "ant:1.4:not-pinned\n" +
        "apache-httpcomponents-client-4-api:4.5.3-2.0:not-pinned\n" +
        "async-http-client:1.7.24.1:not-pinned\n";

    final private String envelopeContent = "{\n" +
        "  \"product\": \"cje\",\n" +
        "  \"version\": \"2.107.3.4\",\n" +
        "  \"distribution\": \"rolling\",\n" +
        "  \"commit\": \"76bc75c3d52602e354594cf412492a9868f3085c\",\n" +
        "  \"core\": \"2.107.3-cb-1\",\n" +
        "  \"plugins\":   {\n" +
        "    \"async-http-client\":     {\n" +
        "      \"name\": \"Async Http Client\",\n" +
        "      \"groupId\": \"org.jenkins-ci.plugins\",\n" +
        "      \"artifactId\": \"async-http-client\",\n" +
        "      \"version\": \"1.7.24.1\",\n" +
        "      \"scope\": \"bootstrap\",\n" +
        "      \"sha1\": \"SC/TzSN+eOrewaDZJZyzLpIQV7E=\",\n" +
        "      \"tier\": \"verified\"\n" +
        "    },\n" +
        "    \"structs\":     {\n" +
        "      \"name\": \"Structs Plugin\",\n" +
        "      \"groupId\": \"org.jenkins-ci.plugins\",\n" +
        "      \"artifactId\": \"structs\",\n" +
        "      \"version\": \"1.14\",\n" +
        "      \"scope\": \"bootstrap\",\n" +
        "      \"sha1\": \"yQrTUhP5jDuBYEyVqyw06Zy9TqA=\",\n" +
        "      \"tier\": \"verified\"\n" +
        "    },\n" +
        "    \"ant\":     {\n" +
        "      \"name\": \"Ant Plugin\",\n" +
        "      \"groupId\": \"org.jenkins-ci.plugins\",\n" +
        "      \"artifactId\": \"ant\",\n" +
        "      \"version\": \"1.8\",\n" +
        "      \"dependencies\": {\"structs\": \"1.6\"},\n" +
        "      \"scope\": \"fat\",\n" +
        "      \"sha1\": \"xwEeOLr2K9b3Vu5TvSI1jQYpMjE=\",\n" +
        "      \"tier\": \"verified\"\n" +
        "    },\n" +
        "    \"durable-task\":     {\n" +
        "      \"name\": \"Durable Task Plugin\",\n" +
        "      \"groupId\": \"org.jenkins-ci.plugins\",\n" +
        "      \"artifactId\": \"durable-task\",\n" +
        "      \"version\": \"1.22\",\n" +
        "      \"scope\": \"fat\",\n" +
        "      \"sha1\": \"CAGRX6lT6zBhwr5XOsYAoPCc7kY=\",\n" +
        "      \"tier\": \"compatible\"\n" +
        "    },\n" +
        "  },\n" +
        "  \"blacklist\":   [\n" +
        "    \"amazon-aws-cli\",\n" +
        "    \"bluesteel-master\",\n" +
        "    \"castle\",\n" +
        "    \"castle-core\",\n" +
        "    \"castle-ebs\",\n" +
        "    \"cjm-feeder\",\n" +
        "    \"cloudbees-cloud-backup\",\n" +
        "    \"cloudbees-credentials\",\n" +
        "    \"cloudbees-enterprise-plugins\",\n" +
        "    \"cloudbees-file-leak-detector\",\n" +
        "    \"cloudbees-github-pull-requests\",\n" +
        "    \"cloudbees-registration\",\n" +
        "    \"operations-center-analytics\",\n" +
        "    \"operations-center-analytics-dashboards\",\n" +
        "    \"operations-center-analytics-feeder\",\n" +
        "    \"operations-center-analytics-viewer\",\n" +
        "    \"operations-center-elasticsearch-provider\",\n" +
        "    \"operations-center-embedded-elasticsearch\",\n" +
        "    \"operations-center-license\",\n" +
        "    \"pse-analytics-dashboards\",\n" +
        "    \"pse-cjoc-server-license\",\n" +
        "    \"pse-config-info\",\n" +
        "    \"tiger-client\",\n" +
        "    \"visual-studio-online\"\n" +
        "  ],\n" +
        "  \"signature\":   {\n" +
        "    \"correct_digest\": \"USolqGOgWlXzLH7dsSAd6AUDvGo=\",\n" +
        "    \"certificates\": [\"MIIDyjCCArICCQDjhYDxdF9w9jANBgkqhkiG9w0BAQUFADCBpjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExETAPBgNVBAcTCFNhbiBKb3NlMRgwFgYDVQQKEw9DbG91ZEJlZXMsIEluYy4xEDAOBgNVBAsTB0plbmtpbnMxGjAYBgNVBAMTEUtvaHN1a2UgS2F3YWd1Y2hpMScwJQYJKoZIhvcNAQkBFhhra2F3YWd1Y2hpQGNsb3VkYmVlcy5jb20wHhcNMTExMDIyMTgzMTM2WhcNMjExMDE5MTgzMTM2WjCBpjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExETAPBgNVBAcTCFNhbiBKb3NlMRgwFgYDVQQKEw9DbG91ZEJlZXMsIEluYy4xEDAOBgNVBAsTB0plbmtpbnMxGjAYBgNVBAMTEUtvaHN1a2UgS2F3YWd1Y2hpMScwJQYJKoZIhvcNAQkBFhhra2F3YWd1Y2hpQGNsb3VkYmVlcy5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDVPr8iuS4BTVjKZ0PvZvfoCkBnNeqVsyjDO5XDdYeGPHbG3D8OIpHZQjgr9IeL+bUF3JEfPS0F0TmNSf0mzrHxGCRsVQcGsDClqFqVnUlG4b9ThcRckYVe+/Wl6DCrGTUI08Ivc7bpnxi+ZxeGtdCGEL6gNyw/t85qs/7JbTbAxXpRs+EWKcC6l8a7N8Uy5xgRxWzPfiF8wuLzrbrJCS0WYV3387eo9RmXqbEVeoAyD7hkcjJb17ZSFv6eJTn0K/Z9i8jmuTqZe6bN/YmP3y4r8tx4z2nx+qFVeJZ7pM7FyRlUAO1qPKjw0pDSGYzoXp3T/1PiBYI09EADQL4dpa5zAgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAKx1keEJnlDHlHCsEEyFdmLxlYJ+6CSlrU9B/pJ7ufUuQJUfCYancmeO+8XSkDzN/3HvpKyWWmUWNlmVvbVnTVxfp3/Y4YshuNfyuP/gXOGoz/ZzQVoq+GJ4gaEr6VZgEv9DCfFQr5HhZQLj1Zc1oQaxB//YrBkw9Yhx7hHSW/qwyrqJEjzA0EEIixNfQm+qdYv4PR1Zzmue9XhGMXpQiymEYo90DJomBPVwVII1OimgIXtDgJQebAxPxvwfhodPNTCxit3zn8Z7I8iMrzHoMA4mk4OJPqwP6Gr4iD8+Fg/xO1T2CxZ4C7gLiQx/OnHCAGxZD2BmGQ9WzzriZ727tl4=\"],\n" +
        "    \"correct_signature\": \"humQnmjIO8dF6kIVAw+4QqS7lbtkOAaz2qIktmXsnh5n6KXJvyU4J7QfUUp1vN9ck823eljc2vXrmmF6WygagdjE6i4RK9vRAus7OtDgX3CaOk5fpcEoN5d0B4c4mBZRyXAjiAm7T7DUSxEbj8pNnWnTEUg6/skUBjj89PZRdD/Hny2qHdiMQ7Q+93iiOpagO2cMO1L3J7XPPPhJE+JN5yBKAl9LivMhF+kcWkBwNGt/pjo8ScR3HKpv6eJNEf/z8SIXBSAn92ug9vgQ3PyyZnhj4x6s9eUNSyxLIJXnISPRG4NS5iBb7mbRuHpgDzKnPMlD7TOFbs2SKtgSYUChsQ==\",\n" +
        "    \"digest\": \"hRn/Bt6ztaWl43IK5sq4eAjyW0Q=\",\n" +
        "    \"signature\": \"pC8rsmrjgvS4HcyfVa/6Luv9omnnO9fWJvyh35sWDfIFhFmevZn6fG3da9EYRjuUF6ZXD5et690Zta++jZXk1yMD2M1tS2zMv//sW/+MVrE5hqkwlbUS3nm0FYBkQHksG7Z0kV+ESmfgtpDnU9eCbjeiR4ilwxXSWoWxC7H+HSKqra8rnJH7clBWBmKNyicDa3/gvbtcAj4WHm2sQfc4ANEpfgtmVihxe9DDZaFb4TYQYRp69ABx7I41ZCMby994gunO4rpt8yzGqRqBiVdxYjtmnxJyO3HnXCL3u/0mVUKtsO3Xeqtew979Lq6o+JGXhfx93nhEgFZ9tSxZ+2DBdA==\"\n" +
        "  }\n" +
        "}";
    final private String csvFileContent =   "Id,Name,Version,Envelope,Version,Type,Scope\n" +
                                            "async-http-client,Async Http Client,1.7.24.1,YES,1.7.24.1,VERIFIED,BOOTSTRAP\n" +
                                            "apache-httpcomponents-client-4-api,,4.5.3-2.0,NO,,,\n" +
                                            "ant,Ant Plugin,1.4,YES,1.8,VERIFIED,FAT\n" +
                                            "active-directory,,2.4,NO,,,\n";
}
