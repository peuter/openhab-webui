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
package org.openhab.ui.cometvisu.internal.backend.model.rss;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link Feed} is used by the CometVisu rss-plugin
 *
 * @author Tobias Bräutigam - Initial contribution
 */
@NonNullByDefault
public class Feed {
    @Nullable
    public String feedUrl;
    public String title;
    @Nullable
    public String link;
    public String author;
    @Nullable
    public String description;
    public String type;
    public List<Entry> entries = new ArrayList<>();

    public Feed(String title, String author, String type) {
        this.title = title;
        this.description = title;
        this.author = author;
        this.type = type;
    }
}
