/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.integrationtests.jupiter.api.extension;

import org.eclipse.jkube.integrationtests.cli.CliUtils;
import org.eclipse.jkube.integrationtests.jupiter.api.DockerRegistry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.eclipse.jkube.integrationtests.cli.CliUtils.isWindows;
import static org.hamcrest.MatcherAssert.assertThat;

public class RegistryExtension extends BaseExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger logger = LoggerFactory.getLogger(RegistryExtension.class);
  @Override
  ExtensionContext.Namespace getNamespace() {
    return ExtensionContext.Namespace.create(RegistryExtension.class);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    final var annotation = context.getRequiredTestClass().getAnnotation(DockerRegistry.class);
    CliUtils.runCommand("docker rm -f " + getName(annotation));
    logger.debug(() -> "Starting Docker Registry Extension");
    final CliUtils.CliResult dockerRegistry;
    if (isWindows()) {
      dockerRegistry = startWindowsDockerRegistry(annotation);
    } else {
      dockerRegistry = startRegularDockerRegistry(annotation);
    }
    assertThat(dockerRegistry.getOutput(), dockerRegistry.getExitCode(), Matchers.equalTo(0));
    logger.debug(() -> "Docker Registry started successfully");
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    logger.debug(() -> "Closing Docker Registry");
    CliUtils.runCommand("docker stop " + getName(context.getRequiredTestClass().getAnnotation(DockerRegistry.class)));
  }

  private static CliUtils.CliResult startRegularDockerRegistry(DockerRegistry dockerRegistry) throws IOException, InterruptedException {
    logger.debug(() -> "Starting standard Docker Registry");
    return CliUtils.runCommand("docker run --rm -d -p " + dockerRegistry.port() +":5000 --name " +
      getName(dockerRegistry) + " registry:2");
  }

  private static CliUtils.CliResult startWindowsDockerRegistry(DockerRegistry dockerRegistry) throws IOException, InterruptedException {
    logger.debug(() -> "Starting Windows specific Docker Registry");
    final var registry = new File("C:\\registry");
    if (!registry.exists() && !registry.mkdirs()) {
      throw new IllegalStateException("Directory C:\\registry cannot be created");
    }
    return CliUtils.runCommand("docker run --rm -d -p " + dockerRegistry.port() +":5000 --name " +
      getName(dockerRegistry) + " -v C:\\registry:C:\\registry marcnuri/docker-registry-windows:ltsc2022");
  }

  private static String getName(DockerRegistry dockerRegistry) {
    return dockerRegistry.containerName() + "-" + dockerRegistry.port();
  }
}
