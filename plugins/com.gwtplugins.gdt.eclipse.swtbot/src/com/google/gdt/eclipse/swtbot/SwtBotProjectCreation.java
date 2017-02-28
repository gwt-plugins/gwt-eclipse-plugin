/**
 *
 */
package com.google.gdt.eclipse.swtbot;

import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public class SwtBotProjectCreation {

  /**
   * Create a basic GWT Java project.
   */
  public static void createJavaStandardProject(SWTWorkbenchBot bot, String projectName, String packageName)
      throws Exception {
    // TODO remove this and use the default sdk
    GwtRuntimeTestUtilities.addDefaultRuntime();

    // Given a gwt sdk is setup
    SwtBotSdkActions.setupGwtSdk(bot);

    // And given a project is created
    SwtBotProjectActions.createWebAppProject(bot, projectName, packageName, true, true);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);
  }

  /**
   * Create a GWT project from Maven Archetype.
   *
   * Archetype: https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic
   */
  public static void createMavenGwtProjectIsCreated1(SWTWorkbenchBot bot, String projectName, String packageName) {
    // And create a maven project using an archetype
    String groupId = projectName;
    String artifactId = projectName;
    String archetypeGroupId = "com.github.branflake2267.archetypes";
    String archetypeArtifactId = "gwt-gpe-test-gwt27-archetype";
    String archetypeVersion = "1.0-SNAPSHOT";
    String archetypeUrl = "https://oss.sonatype.org/content/repositories/snapshots";

    SwtBotProjectActions.createMavenProjectFromArchetype(bot, groupId, artifactId, packageName,
        archetypeGroupId, archetypeArtifactId, archetypeVersion, archetypeUrl);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);
  }

  /**
   * Create a GWT project from Maven Archetype.
   *
   * Archetype: https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic
   */
  public static void createMavenGwtProjectIsCreated2(SWTWorkbenchBot bot, String projectName, String packageName) {
    // And create a maven project using an archetype
    String groupId = packageName;
    String artifactId = projectName;
    String archetypeGroupId = "com.github.branflake2267.archetypes";
    String archetypeArtifactId = "gwt-basic-archetype";
    String archetypeVersion = "2.0-SNAPSHOT";
    String archetypeUrl = "https://oss.sonatype.org/content/repositories/snapshots";

    SwtBotProjectActions.createMavenProjectFromArchetype(bot, groupId, artifactId, packageName,
        archetypeGroupId, archetypeArtifactId, archetypeVersion, archetypeUrl);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);
  }
}
