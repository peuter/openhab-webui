/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.ui.cometvisu.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for the manager backend.
 *
 * @author Tobias Bräutigam - initial contribution
 *
 */
@NonNullByDefault
public class ManagerSettings implements IConfigChangeListener {
    private final Logger logger = LoggerFactory.getLogger(ManagerSettings.class);
    private static ManagerSettings instance = new ManagerSettings();
    private File baseDir = new File(Objects.requireNonNull(Config.cometvisuWebfolder));
    // default config-folder for CometVisu >= 0.12.0
    private File configFolder = new File(
            baseDir.getAbsolutePath() + File.separator + "resource" + File.separator + "config");
    private String resourcesDir = "resource";
    private String designsDir = resourcesDir + File.separator + "designs";
    private String trashFolderName = ".trash";
    private String backupFolderName = "backup";
    private List<Pattern> backupOnChange = new ArrayList<>();
    private Version cometVisuVersion = new Version();
    public static Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)-?(\\w)?$");

    private List<MountPoint> mounts = new ArrayList<>();

    private ManagerSettings() {
        backupOnChange.add(Pattern.compile("visu_config.*\\.xml"));
        Config.addConfigChangeListener(this);
        init();

        // load mounts
        refreshMounts();
    }

    public static ManagerSettings getInstance() {
        return ManagerSettings.instance;
    }

    private void init() {
        // set default version values
        cometVisuVersion.major = 0;
        cometVisuVersion.minor = 12;
        cometVisuVersion.patch = 0;
        cometVisuVersion.dev = false;

        try {
            File versionFile = new File(baseDir.getAbsolutePath() + File.separator + "version");
            Scanner sc = new Scanner(versionFile);
            String versionContent = sc.nextLine();
            sc.close();
            Matcher matcher = ManagerSettings.versionPattern.matcher(versionContent);
            if (matcher.find()) {
                cometVisuVersion.major = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                cometVisuVersion.minor = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
                cometVisuVersion.patch = Integer.parseInt(Objects.requireNonNull(matcher.group(3)));
                cometVisuVersion.dev = "dev".equals(matcher.group(4));
            }
        } catch (FileNotFoundException e) {
            logger.error("{}", e.getMessage());
        }

        if (cometVisuVersion.major == 0 && cometVisuVersion.minor < 11) {
            // non qx version
            configFolder = new File(baseDir.getAbsolutePath() + File.separator + "config");
        }
    }

    private void refreshMounts() {
        mounts.clear();
        // always mount the demo folder
        mounts.add(new MountPoint(Objects.requireNonNull(Paths.get("demo")),
                Objects.requireNonNull(Paths.get("resource", "demo"))));
        // when serving source version, a mount can lookup 2 directory levels
        boolean allowLookup = baseDir.getPath().endsWith("compiled/source");
        Pattern lookupMount = Pattern.compile("^(\\.\\.\\/){0,2}\\w+");

        for (final String target : Config.mountPoints.keySet()) {
            if (!target.contains("..") && !"demo".equalsIgnoreCase(target)) {
                String value = (String) Config.mountPoints.get(target);
                if (value != null) {
                    String[] parts = value.split(":");
                    String source = parts[0];
                    if (!source.contains("..") || (allowLookup && lookupMount.matcher(source).find())) {
                        boolean writeable = parts.length > 1 && parts[1].contains("w");
                        boolean showSubDirs = parts.length > 1 && parts[1].contains("s");
                        if (source.startsWith(File.separator)) {
                            source = source.substring(1);
                        }
                        MountPoint mount = new MountPoint(Objects.requireNonNull(Paths.get(target)),
                                Objects.requireNonNull(Paths.get(source)), showSubDirs, writeable);
                        mounts.add(mount);
                    }
                }
            }
        }
    }

    /**
     *
     * @return The path where the CometVisu files are installed
     */
    public Path getCometVisuPath() {
        return baseDir.toPath();
    }

    /**
     * @return The absolute path to the cometvisus resource folder
     */
    public String getResourcesDir() {
        return configFolder.getAbsoluteFile() + File.separator + resourcesDir;
    }

    /**
     *
     * @return the internal mount points used for the manager
     */
    public List<MountPoint> getMounts() {
        return mounts;
    }

    /**
     *
     * @return the cometvisu's config folder
     */
    public File getConfigFolder() {
        return configFolder;
    }

    public String getBackupPath() {
        return configFolder.getAbsoluteFile() + File.separator + backupFolderName + File.separator;
    }

    public String getTrashFolder() {
        return trashFolderName;
    }

    public String getTrashPath() {
        return configFolder.getAbsoluteFile() + File.separator + trashFolderName + File.separator;
    }

    public String getDesignsDir() {
        return designsDir;
    }

    public File getDesignFolder() {
        return new File(baseDir, Objects.requireNonNull(designsDir));
    }

    public List<Pattern> getBackupOnChange() {
        return backupOnChange;
    }

    public Version getCometVisuVersion() {
        return cometVisuVersion;
    }

    public void setResourcesDir(String resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    @Override
    public void handleConfigChange(String key) {
        if (key != null) {
            if (key.equals(Config.COMETVISU_WEBFOLDER_PROPERTY)) {
                baseDir = new File(Objects.requireNonNull(Config.cometvisuWebfolder));
            } else if (key.equals(Config.COMETVISU_MOUNTPOINTS)) {
                refreshMounts();
            }
        }
    }

    public Path getConfigPath() {
        return configFolder.toPath();
    }
}
