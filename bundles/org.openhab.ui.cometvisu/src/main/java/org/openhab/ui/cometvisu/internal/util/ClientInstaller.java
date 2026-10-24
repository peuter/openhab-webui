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
package org.openhab.ui.cometvisu.internal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.ui.cometvisu.internal.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Utility class for CometVisu client availability checks. It also provides methods to download
 * the CometVisu client if it does not exist in the configured COMETVISU_WEBFOLDER
 *
 * @author Tobias Bräutigam - Initial contribution
 *
 */
@NonNullByDefault
public class ClientInstaller {

    private final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);

    private static final ClientInstaller INSTANCE = new ClientInstaller();

    /** the timeout to use for connecting to a given host (defaults to 5000 milliseconds) */
    private int timeout = 5000;

    /** URL for releases in github API */
    private static final String RELEASE_URL = "https://api.github.com/repos/CometVisu/CometVisu/releases";

    private static final byte[] BUFFER = new byte[0xFFFF];

    @Nullable
    private Map<String, Object> latestRelease;

    /** Regular expression to parse semver version strings */
    private final Pattern semverPattern = Pattern.compile("\\D*([\\d]{1,3})\\.(\\d{1,3})\\.(\\d{1,3}).*");

    private List<String> alreadyCheckedFolders = new ArrayList<>();

    private boolean downloadAvailableButBlocked = false;

    private ClientInstaller() {
    }

    public static ClientInstaller getInstance() {
        return INSTANCE;
    }

    /**
     * Check the webfolder for existance and if there is a cometvisu in it.
     * Create the folder and download the cometvisu otherwise
     */
    public void check() {
        this.check(false);
    }

    /**
     * Check the webfolder for existance and if there is a cometvisu in it.
     * Create the folder and download the cometvisu otherwise
     *
     * @param force {boolean} force downloading if client not found
     */
    public void check(boolean force) {
        if (Config.cometvisuWebfolder.isEmpty()) {
            throw new IllegalArgumentException("cometvisuWebfolder must not be null or empty");
        }
        if (alreadyCheckedFolders.contains(Config.cometvisuWebfolder) && !force) {
            // this folder has been checked already
            logger.debug("web folder {} has already been checked", Config.cometvisuWebfolder);
            return;
        }
        if (!force) {
            // do not add the forced checks
            alreadyCheckedFolders.add(Objects.requireNonNull(Config.cometvisuWebfolder));
        }

        File webFolder = new File(Objects.requireNonNull(Config.cometvisuWebfolder));
        if (!webFolder.exists()) {
            logger.debug("creating cometvisu webfolder {}", webFolder.getAbsolutePath());
            webFolder.mkdirs();
        }
        if (webFolder.isDirectory()) {
            // check for cometvisu either we have a index.html in it (for releases) or we have a package.json in it (for
            // source versions)
            if (!new File(webFolder, "index.html").exists() || !new File(webFolder, "package.json").exists()) {
                // no cometvisu found if folder is empty download the cometvisu
                if (Config.cometvisuAutoDownload || force) {
                    downloadLatestRelease();
                } else {
                    downloadAvailableButBlocked = true;
                    logger.error("No CometVisu client found in '{}' and automatic download is disabled. "
                            + "Please enable this feature by setting 'autoDownload=true' in your 'services/cometvisu.cfg' file.",
                            webFolder.getAbsolutePath());
                }
            } else {
                // check for upgrades
                Map<String, Object> latestRelease = getLatestRelease();
                if (latestRelease == null) {
                    logger.error("no latest release found");
                    return;
                }

                // find version in local client
                File version = findClientRoot(webFolder, "version");

                if (version.exists()) {
                    try {
                        String currentVersion = Files.readString(version.toPath()).trim();
                        String currentRelease = (String) latestRelease.getOrDefault("tag_name", "");
                        if (currentRelease.startsWith("v")) {
                            currentRelease = currentRelease.substring(1);
                        }
                        if (isNewer(currentRelease, currentVersion)) {
                            logger.info("CometVisu should be updated to version {}, you are using version {}",
                                    currentRelease, currentVersion);
                        }
                    } catch (IOException e) {
                        logger.error("error reading version from installed CometVisu client: {}", e.getMessage(), e);
                    }
                }
            }
        } else {
            logger.error("webfolder {} is no directory", webFolder.getAbsolutePath());
        }
    }

    /**
     * Search for a file in the known file structure of the CometVisu client.
     * The search oder is:
     *
     * . # for releases
     * build/ # for CometVisu >= 0.11 with generated build
     * source/ # for CometVisu >= 0.11 without generated build (source version)
     * src/ # for CometVisu < 0.11 (source version)
     *
     * @param webFolder {File} folder to start the search
     * @param fileName {String} Filename to lookup
     * @return
     */
    public static File findClientRoot(File webFolder, String fileName) {
        if (!webFolder.exists() || !webFolder.isDirectory()) {
            throw new IllegalArgumentException("webFolder must be an existing directory");
        }
        File file = new File(webFolder, fileName);
        if (!file.exists()) {
            File build = new File(webFolder, "build");
            File source = new File(webFolder, "source");
            File src = new File(webFolder, "src");
            if (build.exists()) {
                // new CometVisu client >= 0.11.x
                file = new File(build, fileName);
            } else if (source.exists()) {
                // new CometVisu client >= 0.11.x
                file = new File(source, fileName);
            } else if (src.exists()) {
                file = new File(src, fileName);
            }
        }
        return file;
    }

    /**
     * Checks if the CometVisu client is missing, but the automatic download is blocked by configuration.
     *
     * @return {boolean}
     */
    public boolean isDownloadAvailableButBlocked() {
        return downloadAvailableButBlocked;
    }

    /**
     * Compare two SemVer version strings and return true if releaseVersion is newer then currentVersion
     *
     * @param releaseVersion {String} e.g 0.11.0
     * @param currentVersion {String} e.g. 0.10.0
     * @return {Boolean}
     */
    private boolean isNewer(String releaseVersion, String currentVersion) throws NumberFormatException {
        logger.debug("checking if {} is newer than {}", releaseVersion, currentVersion);
        Matcher release = semverPattern.matcher(releaseVersion);
        Matcher current = semverPattern.matcher(currentVersion);
        if (!release.matches()) {
            throw new NumberFormatException("release version format error " + releaseVersion);
        }
        if (!current.matches()) {
            throw new NumberFormatException("current version format error " + currentVersion);
        }

        boolean equal = true;

        for (int i = 1; i <= 3; i++) {
            int relVer = Integer.parseInt(java.util.Objects.requireNonNull(release.group(i)));
            int curVer = Integer.parseInt(java.util.Objects.requireNonNull(current.group(i)));
            if (relVer > curVer) {
                return false;
            } else if (relVer < curVer) {
                equal = false;
            }
        }
        return !equal;
    }

    /**
     * Fetch the latest release description for the CometVisu project from github
     *
     * @return {Map}
     */
    private @Nullable Map<String, Object> getLatestRelease() {
        if (latestRelease == null) {
            Properties headers = new Properties();
            headers.setProperty("Accept", "application/json");
            try {
                String response = HttpUtil.executeUrl("GET", RELEASE_URL, headers, null, null, timeout);
                if (response == null) {
                    logger.error("No response received from '{}'", RELEASE_URL);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> jsonResponse = new Gson().fromJson(response, ArrayList.class);

                    if (jsonResponse == null || jsonResponse.isEmpty()) {
                        logger.error("No releases found in the response from '{}'", RELEASE_URL);
                        return null;
                    }
                    // releases are ordered top-down, the latest release comes first
                    for (Map<String, Object> release : jsonResponse) {
                        if (!((boolean) release.getOrDefault("prerelease", true))
                                && !((boolean) release.getOrDefault("draft", true))) {
                            latestRelease = release;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("error downloading release data: {}", e.getMessage(), e);
            }
        }
        return latestRelease;
    }

    /**
     * Download the latest release and extract it to the configured COMETVISU_WEBFOLDER
     */
    public void downloadLatestRelease() {
        // request the download URL for the latest CometVisu release from the github API
        @Nullable
        Map<String, Object> latestRelease = getLatestRelease();
        if (latestRelease == null) {
            logger.error("no latest release found");
            return;
        }
        Object assetsResponse = latestRelease.get("assets");

        Map<String, Object> releaseAsset = null;
        if (!(assetsResponse instanceof List<?> assets) || assets.isEmpty()) {
            logger.error("no assets found for release {}", latestRelease.get("name"));
            return;
        }
        for (Object assetResponse : assets) {
            if (!(assetResponse instanceof Map<?, ?> asset)) {
                continue;
            }
            String contentType = asset.get("content_type") instanceof String type ? type : "";
            String name = asset.get("name") instanceof String assetName ? assetName : "";
            if ("application/zip".equalsIgnoreCase(contentType)
                    || ("binary/octet-stream".equalsIgnoreCase(contentType) && name.endsWith(".zip"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedAsset = (Map<String, Object>) asset;
                releaseAsset = typedAsset;
                break;
            }
        }
        if (releaseAsset == null) {
            logger.error("no zip download file found for release {}", latestRelease.get("name"));
        } else {
            File releaseFile = new File("release.zip");
            try {
                URL url = new URI((String) releaseAsset.get("browser_download_url")).toURL();
                try (FileOutputStream fos = new FileOutputStream(releaseFile)) {
                    url.openStream().transferTo(fos);
                    fos.flush();
                }

                ZipFile zip = new ZipFile(releaseFile, ZipFile.OPEN_READ);
                extractFolder("cometvisu/release/", zip, Objects.requireNonNull(Config.cometvisuWebfolder));
            } catch (IOException e) {
                logger.error("error opening release zip file {}", e.getMessage(), e);
            } catch (URISyntaxException e) {
                logger.error("error creating URL from release asset {}", e.getMessage(), e);
            } finally {
                if (releaseFile.exists()) {
                    releaseFile.delete();
                }
            }
        }
    }

    /**
     * Extract the content of the zip file to the target folder
     *
     * @param folderName {String} subfolder inside the zip file that should be extracted
     * @param zipFile {ZipFile} zip-file to extract
     * @param destDir {String} destination for the extracted files
     */
    private void extractFolder(String folderName, ZipFile zipFile, String destDir) {
        if (folderName.isEmpty()) {
            logger.error("folderName is null or empty");
            return;
        }
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (entry != null && entry.getName().startsWith(folderName)) {
                String target = entry.getName().substring(folderName.length());
                File file = new File(destDir, target);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    if (file.exists() && file.getPath().matches(".*/config/visu_config.*\\.xml")) {
                        // never ever overwrite existing config files
                        continue;
                    }
                    String parent = file.getParent();
                    if (parent != null) {
                        new File(parent).mkdirs();
                    }

                    try (InputStream is = zipFile.getInputStream(entry); OutputStream os = new FileOutputStream(file)) {
                        for (int len; (len = is.read(BUFFER)) != -1;) {
                            os.write(BUFFER, 0, len);
                        }
                        logger.info("extracted zip file {} to folder {}", zipFile.getName(), destDir);
                    } catch (IOException e) {
                        logger.error("error extracting file {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
}
