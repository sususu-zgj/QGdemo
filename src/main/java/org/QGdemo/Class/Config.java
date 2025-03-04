package org.QGdemo.Class;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Properties;

public class Config {
    //private final String configPath = "src/main/resources/config.properties";
    private final String configPath = "config.properties";
    private String mysqlIp;
    private String mysqlPort;
    private String databaseName;
    private String mysqlUsername;
    private String mysqlPassword;
    private String adminUsername;
    private String adminPassword;
    private String encoding;

    private boolean loaded;

    public Config() {
        Properties prop = new Properties();
        createConfig();
        try(FileInputStream file = new FileInputStream(configPath)) {
            prop.load(file);
            mysqlIp = prop.getProperty("database.ip");
            mysqlPort = prop.getProperty("database.port");
            databaseName = prop.getProperty("database.name");
            mysqlUsername = prop.getProperty("database.username");
            mysqlPassword = prop.getProperty("database.password");
            adminUsername = prop.getProperty("admin.username");
            adminPassword = prop.getProperty("admin.password");
            encoding = prop.getProperty("encoding");
            loaded = true;
        } catch (IOException e) {
            loaded = false;
        }
    }

    private void createConfig() {
        String configName = "config.properties";
        File configFile = new File(getJarDirectory(), configName);

        if(!configFile.exists()) {
            try(InputStream stream = Config.class.getResourceAsStream("/" + configName)) {
                if(stream != null) {
                    Files.copy(stream, configFile.toPath());
                }
            } catch (IOException e) {
                System.out.println("复制配置文件失败");
            }
        }
    }

    private File getJarDirectory() {
        try {
            File file = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            return file.getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("定位jar文件时出错", e);
        }
    }

    public String getMysqlIp() {
        return mysqlIp;
    }

    public String getMysqlPort() {
        return mysqlPort;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
