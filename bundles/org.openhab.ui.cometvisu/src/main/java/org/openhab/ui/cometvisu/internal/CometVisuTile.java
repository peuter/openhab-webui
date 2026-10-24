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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.ui.tiles.Tile;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author Tobias Bräutigam - Initial contribution
 * @author Yannick Schaus - remove dependency to dashboard
 * @author Mark Herwege - Implement sitemap registry
 */
@Component(service = Tile.class, immediate = true)
@NonNullByDefault
public class CometVisuTile implements Tile {

    @Override
    public String getName() {
        return "CometVisu";
    }

    @Override
    public String getUrl() {
        String path = "/";
        // lets find the index.html
        File root = new File(Objects.requireNonNull(Config.cometvisuWebfolder));
        File index = new File(root, "index.html");
        if (index.exists()) {
            path = "/index.html";
        } else {
            File build = new File(root, "build");
            if ((new File(build, "index.html")).exists()) {
                path = "/build/index.html";
            } else {
                File source = new File(root, "source");
                if ((new File(source, "index.html")).exists()) {
                    path = "/source/index.html";
                } else {
                    File src = new File(root, "src");
                    if ((new File(src, "index.html")).exists()) {
                        path = "/src/index.html";
                    }
                }
            }
        }
        return Config.cometvisuWebappAlias + path;
    }

    @Override
    public @Nullable String getOverlay() {
        return "html5";
    }

    @Override
    public String getImageUrl() {
        return "/res/img/cometvisu.png";
    }
}
