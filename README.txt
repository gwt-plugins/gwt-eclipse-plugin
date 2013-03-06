This file contains instructions on how to set up your Eclipse environment to
work with the source code for the Google Plugin for Eclipse:

Configuring Your Eclipse Workspace
==================================

Configuring your Eclipse workspace to develop on the Google Eclipse plugin is very
much like getting set up to develop for GWT. The same style rules apply, and
Eclipse projects are provided for you to import. The "settings" directory
mentioned below is under "eclipse", in your checkout of plugin.

Macintosh users: Note that on the Macintosh version of Eclipse, "Preferences"
is under the "Eclipse" menu, not under "Window".

------------- Dependent Plugins -----------

In Eclipse 3.3/3.4:

Copy the plugin JARs from tools/swtbot/3.3 into your Eclipse's /dropins directory (on 3.4)
or /plugins directory (on 3.3).  You'll need to restart Eclipse to detect the new plugins.

In Eclipse 3.5/3.6/3.7:

Copy the plugin JARs from tools/swtbot/3.5 into your Eclipse's /dropins directory.

You'll need to restart Eclipse to detect the new plugins.

The com.google.gdt.eclipse.maven plugin requires M2Eclipse (Maven support) to be installed.

The com.google.gdt.eclipse.gph.hge project requires MercurialEclipse to be installed. You can either
close the project, or install MercurialEclipse: http://cbes.javaforge.com/update.

The com.google.gdt.eclipse.gph.subclipse project requires Subclipse to be installed. You can either
close the project, or install Subclipse: http://subclipse.tigris.org/update_1.6.x.

The com.google.gdt.eclipse.gph.subversive project requires Subversive to be installed. You can either
close the project, or install Subversive: http://download.eclipse.org/releases/helios (under Collaboration).

------------- Text Editors ----------------

Window->Preferences->General->Editors->Text Editors
Make sure that "Displayed Tab Width" is set to 2
Enable "Insert Spaces for Tabs"
Enable "Show Print Margin" and set "Print Margin Column" to 80

------------- XML Files -------------------

Window->Preferences->Web and XML->XML Files->Source
(or Window->Preferences->XML->XML Files->Editor, if you can't find it there)
Set "Line Width" 80
Enable "Split Multiple Attributes Each of a New Line"
Enable "Indent Using Spaces" with an Indentation Size of 4

------------- Ant Build Files -------------

Window->Preferences->Ant->Editor->Formatter
Set "Tab Size" to 4
Disable "Use Tabs Instead of Spaces"
Set "Maximum Line Width" to 80
Enable "Wrap Long Element Tags"

---------------- Spelling -----------------

Window->Preferences->General->Editors->Text Editors->Spelling
Enable spell checking
Use "settings/english.dictionary".

-------------Classpath Variables -----------

For GPE 2.3 and after, this step is no longer necessary. See CL/20212038

Window->Preferences->Java->Build Path->Classpath Variables

Define the classpath variable "GAE_TOOLS_JAR" which points to the appengine-api-tools.jar file that is part of an App Engine SDK.

------------ Output Filtering --------------

Window->Preferences->Java->Compiler->Building
Make sure "Filtered Resources" includes ".svn/"

----------- Code Templates ----------------

Window->Preferences->Java->Code Style->Code Templates

Comments->Files template should look like this:

/*
 * Copyright ${year} Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

Comments->Types template should look like this:

/**
 *
 * ${tags}
 */

---------- Save Actions -------------------

Window->Preferences->Java->Editor->Save Actions

Enable "Perform the Selected Actions on Save"
Enable "Format Source Code"
Enable "Organize Imports"
Enable "Additional Actions"
Click "Configure", and make sure that all actions are disabled except "Sort Members Excluding fields, enum constants, and initializers"

---------- Code style/formatting ----------

Window->Preferences->Java->Code Style->Formatter->Import...
  settings/code-style/gwt-format.xml

----------- Import organization -----------

Window->Preferences->Java->Code Style->Organize Imports->Import...
  settings/code-style/gwt.importorder

------------ Member sort order ------------

Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, so make your settings match:
  settings/code-style/gwt-sort-order.png

First, members should be sorted by category.
1) Types
2) Static Fields
3) Static Initializers
4) Static Methods
5) Fields
6) Initializers
7) Constructors
8) Methods

Second, members in the same category should be sorted by visibility.
1) Public
2) Protected
3) Default
4) Private

Third, within a category/visibility combination, members should be sorted
alphabetically.

----------- Mylyn -----------

Only required for Eclipse 3.7
Go to Help->Install New Software
From the drop down list, select Google Internal for Eclipe 3.7
Uncheck Group Items by Category
Install Mylyn Commons
If you are not using the internal version of Eclipse, please install Mylyn Commons from the Indigo Update site.

== Checkstyle ==
Checkstyle is used to enforce good programming style.

1. Install Checkstyle plugin v4.x:
   Download this from http://sourceforge.net/projects/eclipse-cs/files/Eclipse%20Checkstyle%20Plug-in/v4.4.2/com.atlassw.tools.eclipse.checkstyle_4.4.2-bin.zip/download  
   Copy plugins/com.atlassw.tools.eclipse.checkstyle_4.4.2 from the extraction
   of the above downloaded zip file into the dropins directory of your eclipse
   installation.

2. Enable Custom GWT Checkstyle checks:

Copy "settings/code-style/gwt-customchecks.jar" into:
  <eclipse>/plugins/com.atlassw.tools.eclipse.checkstyle_x.x.x/extension-libraries

3. Import GWT Checks:

Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "GWT Checks" (important)
Set the location to "settings/code-style/gwt-checkstyle.xml".
Suggested: Check "Protect Checkstyle configuration file".
Click "Ok".

4. Import GWT Checks for Tests

Repeat step 3, except:
Set the Name to "GWT Checks for Tests" (important)
Set the location to "settings/code-style/gwt-checkstyle-tests.xml".

== Importing the Google Plugin projects ==

Having set up your workspace appropriately, you can now import the appropriate
projects.

File -> Import -> General -> Existing Projects into Workspace

Select your checkout of the trunk of Google Eclipse Plugin to see all of the
Eclipse projects for you to import. You should only import the projects that
correspond to the version of Eclipse that you are using for development, and
the platform you are running on. For example, if you have Eclipse 3.4, do not
import a project that has "3.3" in its name. As another example, if you are
running on Windows, do not import projects that have readme "win32" or "macosx"
in their name. 

== Launching the Plugin ==

Once your projects have been imported, go to the Package Explorer and
right-click on the "com.google.gdt.eclipse.suite" project. Go to 
"Run As" > "Eclipse Application".  Another instance of Eclipse should launch,
running GPE!

Note: Setting these two environment variables will cause the GWT and App Engine
SDKs to be registered as GPE SDK Bundles. However, in development mode, this
only happens when the workbench metadata is first created. To have the workbench
pick up changes to these variables,  go to the Main tab in your launch
configuration, and check 'Clear' under Workspace Data. Note that this will also
remove any projects that you created in the runtime workbench.

