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
package org.openhab.ui.cometvisu.internal.backend.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.ui.cometvisu.internal.Config;
import org.openhab.ui.cometvisu.internal.util.FileOperationException;
import org.openhab.ui.cometvisu.internal.util.FsUtil;
import org.openhab.ui.cometvisu.internal.util.MountedFile;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Copies files or folders for the CometVisu manager.
 *
 * @author Tobias Bräutigam - Initial contribution
 * @author Wouter Born - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(Config.COMETVISU_BACKEND_ALIAS + "/fs/copy")
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@RolesAllowed({ Role.USER, Role.ADMIN })
@Path(Config.COMETVISU_BACKEND_ALIAS + "/fs/copy")
@Tag(name = Config.COMETVISU_BACKEND_ALIAS + "/fs/copy")
@NonNullByDefault
public class CopyResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(CopyResource.class);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Copy folder or file to a new place", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "forbidden"),
            @ApiResponse(responseCode = "404", description = "not found"),
            @ApiResponse(responseCode = "405", description = "copy failed"),
            @ApiResponse(responseCode = "406", description = "target exists") })
    public Response copy(
            @Parameter(description = "current path of the filesystem entry", required = true) @QueryParam("src") String src,
            @Parameter(description = "new path of the filesystem entry", required = true) @QueryParam("target") String target,
            @Parameter(description = "replace existing file/folder") @DefaultValue("false") @QueryParam("force") Boolean force) {
        try {
            MountedFile sourceFile = new MountedFile(src);
            MountedFile targetFile = new MountedFile(target);
            if (!sourceFile.exists()) {
                return FsUtil.createErrorResponse(Status.NOT_FOUND, "source not found");
            }
            if (targetFile.isReadonlyMount()) {
                return FsUtil.createErrorResponse(Status.FORBIDDEN, "not allowed");
            }
            java.nio.file.Path source = sourceFile.getAbsolutePath();
            java.nio.file.Path dest = targetFile.getAbsolutePath();
            if (targetFile.exists()) {
                if (!force) {
                    return FsUtil.createErrorResponse(Status.NOT_ACCEPTABLE, "target exists");
                }
                if (source.equals(dest) || targetFile.isDirectory() != sourceFile.isDirectory()
                        || dest.startsWith(source)) {
                    return FsUtil.createErrorResponse(Status.NOT_ACCEPTABLE, "target exists");
                }
            } else if (dest.startsWith(source)) {
                return FsUtil.createErrorResponse(Status.NOT_ACCEPTABLE, "cannot copy a folder into itself");
            }
            java.nio.file.Path parentTarget = dest.getParent();
            if (!parentTarget.toFile().exists()) {
                parentTarget.toFile().mkdirs();
            }
            if (!parentTarget.toFile().canWrite()) {
                return FsUtil.createErrorResponse(Status.FORBIDDEN, "not allowed");
            }
            copyRecursively(source, dest);
            return Objects.requireNonNull(Response.ok().build());
        } catch (FileOperationException e) {
            return FsUtil.createErrorResponse(e);
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            return FsUtil.createErrorResponse(Status.METHOD_NOT_ALLOWED, "copy failed");
        }
    }

    private void copyRecursively(java.nio.file.Path source, java.nio.file.Path dest) throws IOException {
        try (Stream<java.nio.file.Path> paths = Files.walk(source).map(Objects::requireNonNull)) {
            for (java.nio.file.Path path : (Iterable<java.nio.file.Path>) paths::iterator) {
                java.nio.file.Path current = dest.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(current);
                } else {
                    Files.copy(path, current, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
