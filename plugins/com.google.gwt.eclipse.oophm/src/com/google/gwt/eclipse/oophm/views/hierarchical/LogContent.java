/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gwt.eclipse.oophm.views.hierarchical;

import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.IModelNode;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.Log;
import com.google.gwt.eclipse.oophm.model.LogContentProvider;
import com.google.gwt.eclipse.oophm.model.LogEntry;
import com.google.gwt.eclipse.oophm.model.LogLabelProvider;
import com.google.gwt.eclipse.oophm.model.LogEntry.Data;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.console.JavaStackTraceHyperlink;
import org.eclipse.jdt.internal.formatter.comment.Java2HTMLEntityReader;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The content pane for the log entries.
 * 
 * @param <T> The entity (launch configuration, browser, or server) associated
 *          with the log
 */
@SuppressWarnings("restriction")
public class LogContent<T extends IModelNode> extends Composite {

  /**
   * Re-use the existing functionality for generating hyperlinks in stack traces
   * in console output. Main difference is that we explicitly specify the URL,
   * whereas our superclass extracts it from the console document.
   */
  private static class DevModeStackTraceHyperlink extends
      JavaStackTraceHyperlink {

    private final String url;

    public DevModeStackTraceHyperlink(String url, TextConsole console) {
      super(console);
      assert (url.startsWith(JAVA_SOURCE_URL_PREFIX));
      url = url.substring(JAVA_SOURCE_URL_PREFIX.length());

      try {
        url = URLDecoder.decode(url, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // Should never happen, but if it did, then presumably encoding failed
        // as well, so ignore
      }
      this.url = url;
    }

    @Override
    protected String getLinkText() {
      return url;
    }
  }

  /**
   * Matches GWT TreeLogger messages with a reference to a Java source file.
   * 
   * Regex groups: 1) Absolute filesystem path to Java source. Its format can be
   * passed directly to the {@link Path#Path(String)} constructor.
   * 
   * Example: Errors in 'file:/eclipse/Hello/src/com/example/client/Gwt.java'
   */
  private static final Pattern GWT_ERROR_FILE_REGEX = Pattern.compile(".*'file:(.+\\.java)'");

  /**
   * Matches GWT TreeLogger messages with a reference to a line number. These
   * errors are nested below errors referencing a file (if the file is Java, the
   * parent error should match {@link LogContent#GWT_ERROR_FILE_REGEX}.
   * 
   * Regex groups: 1) The part of the line that should be enclosed in a
   * hyperlink, 2) The line number, and 3) The rest of the line, which is not
   * part of the link.
   * 
   * Example: Line 22: The type Gwt must implement the inherited abstract method
   * EntryPoint.onModuleLoad()
   */
  private static final Pattern GWT_ERROR_LINE_REGEX = Pattern.compile("^(Line (\\d+))(:.*)");

  /**
   * Made-up URL prefix for an address to a line in Java source code. We define
   * this to differentiate these types of links from external HTTP links.
   */
  private static final String JAVA_SOURCE_URL_PREFIX = "java://";

  /**
   * Matches a line in a Java stack trace.
   * 
   * Regex groups: 1) the URL (minus the prefix) to the Java source location,
   * and 2) the part of the line that should be hyperlinked.
   * 
   * Example: at com.example.client.Gwt.onModuleLoad(Gwt.java:50)
   */
  private static final Pattern JAVA_STACK_FRAME_REGEX = Pattern.compile("^\\s*at ([^\\(]+\\((.+\\.java:\\d+)\\))$");

  /**
   * Returns the HTML markup for a hyperlink to a line in a Java source file.
   * 
   * @param javaSourceAddress the location to the Java source line, specified in
   *          the following format: com.example.Class.method(Class.java)
   * @param text the hyperlink text
   */
  private static String buildJavaSourceHyperlink(String javaSourceAddress,
      String text) {
    if (javaSourceAddress == null) {
      // Shouldn't happen, but if we don't have a source address, just return
      // the text by itself (without a link).
      return convertToHtmlContent(text);
    }

    StringBuffer buf = new StringBuffer();
    buf.append("<a href=\"");
    buf.append(JAVA_SOURCE_URL_PREFIX);

    try {
      buf.append(URLEncoder.encode(javaSourceAddress, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // If we fail, just use the un-encoded URL
      buf.append(javaSourceAddress);
    }

    buf.append("\">");
    buf.append(convertToHtmlContent(text));
    buf.append("</a>");
    return buf.toString();
  }

  private static String collapseHtmlFormatting(String html) {
    html = html.replace("<br/>", "\n");
    html = html.replaceAll("<[^<>]+>", "");
    return html;
  }

  /**
   * Computes the address of a particular line in a Java source file, in the
   * format accepted by
   * {@link LogContent#buildJavaSourceHyperlink(String, String)}.
   */
  private static String computeJavaSourceAddress(String filesystemPath,
      String lineNumber) {
    IPath path = new Path(filesystemPath);
    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
        path);
    if (file != null && file.exists()) {
      ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
      if (cu != null) {
        IJavaElement cuParent = cu.getParent();
        if (cuParent instanceof IPackageFragment) {
          IPackageFragment pckgFragment = (IPackageFragment) cuParent;

          StringBuffer sb = new StringBuffer();
          sb.append(pckgFragment.getElementName());
          /*
           * We can use anything for the type and method, since
           * JavaStackTraceHyperlink will strip it out anyway (it only uses the
           * package name, the compilation unit name, and the line number).
           */
          sb.append(".DummyType.dummyMethod");
          sb.append('(');
          sb.append(cu.getElementName());
          sb.append(':');
          sb.append(lineNumber);
          sb.append(')');
          return sb.toString();
        }
      }
    }
    return null;
  }

  private static String convertToHtmlContent(String unescapedStr) {
    Java2HTMLEntityReader reader = new Java2HTMLEntityReader(new StringReader(
        unescapedStr));
    try {
      String escapedString = reader.getString();
      /*
       * NOTE: The expansion of '\t' into four spaces does not technically
       * belong here. We are assuming that whatever is rendering the HTML
       * content will preserve whitespace. There might be a better way to deal
       * with this.
       */
      escapedString = escapedString.replaceAll("\t", "    ");
      escapedString = escapedString.replaceAll("\n", "<br/>");

      // the html renderer doesn't understand &circ; or &tilde; by themselves
      escapedString = escapedString.replaceAll("&circ;", "^");
      escapedString = escapedString.replaceAll("&tilde;", "~");
      return escapedString;
    } catch (IOException e) {
      return unescapedStr;
    }
  }

  private final Log<T> log;

  private FilteredTree logEntries;

  private LogLabelProvider<T> logLabelProvider;
  private ScrolledFormText scrolledFormDetailsText;
  private String scrolledFormDetailsTextContents;

  private TreeViewer treeViewer;

  public LogContent(Composite parent, Log<T> log) {
    super(parent, SWT.NONE);

    this.log = log;

    setLayout(new FillLayout());
    SashForm sashForm = new SashForm(this, SWT.VERTICAL);

    createViewer(sashForm);
    createDetailsPane(sashForm, treeViewer.getTree().getBackground());

    sashForm.setWeights(new int[] {70, 30});

    revealChildrenThatNeedAttention(treeViewer, log.getRootLogEntry());

    LogEntry<T> entryToSelect = log.getFirstDeeplyNestedChildWithMaxAttn();
    if (entryToSelect != null) {
      treeViewer.setSelection(new StructuredSelection(entryToSelect));
    }
  }

  private String buildDetailsHtml(String content) {
    StringBuffer buf = new StringBuffer();
    for (String line : content.split("\\n")) {
      // Add hyperlinks to Java stack traces
      Matcher matcher = JAVA_STACK_FRAME_REGEX.matcher(line);
      if (matcher.matches()) {
        buf.append(convertToHtmlContent(line.substring(0, matcher.start(2))));
        buf.append(buildJavaSourceHyperlink(matcher.group(1), matcher.group(2)));
        buf.append(convertToHtmlContent(line.substring(matcher.end(2))));
      } else {
        buf.append(convertToHtmlContent(line));
      }
      buf.append("<br/>");
    }
    return buf.toString();
  }

  private String buildLabelHtml(LogEntry<?> logEntry) {
    String label = logEntry.getLogData().getLabel();
    LogEntry<?> parentLogEntry = logEntry.getParent();
    if (parentLogEntry != null) {
      Data parentData = parentLogEntry.getLogData();
      if (parentData != null) {
        String parentLabel = parentData.getLabel();

        Matcher matcher = GWT_ERROR_LINE_REGEX.matcher(label);
        Matcher parentMatcher = GWT_ERROR_FILE_REGEX.matcher(parentLabel);

        // If the log entry is a GWT error with a line number, link to the
        // offending line in the Java source.
        if (matcher.matches() && parentMatcher.matches()) {
          String address = computeJavaSourceAddress(parentMatcher.group(1),
              matcher.group(2));
          if (address != null) {
            return buildJavaSourceHyperlink(address, matcher.group(1))
                + convertToHtmlContent(matcher.group(3));
          }
        }
      }
    }

    // No hyperlinks to insert
    return convertToHtmlContent(label);
  }

  private void copyToClipboard(String text) {
    Clipboard clipboard = new Clipboard(getDisplay());
    TextTransfer tt = TextTransfer.getInstance();

    clipboard.setContents(new Object[] {text}, new Transfer[] {tt});
  }

  private void copyTreeSelectionToClipboard() {
    ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
    TreePath[] paths = selection.getPaths();

    StringBuffer buf = new StringBuffer();

    for (TreePath path : paths) {
      LogEntry<?> entry = (LogEntry<?>) path.getLastSegment();
      buf.append(createTabString(path.getSegmentCount() - 1));
      buf.append(entry.toString());
      buf.append("\n");
    }

    if (buf.length() > 0) {
      buf.deleteCharAt(buf.length() - 1); // take off last \n
    }

    copyToClipboard(buf.toString());
  }

  private void createDetailsPane(Composite parent, Color backgroundColor) {
    scrolledFormDetailsText = new ScrolledFormText(parent, SWT.V_SCROLL
        | SWT.H_SCROLL | SWT.BORDER, false);
    /*
     * If focusable, the FormText will jump to its top-most hyperlink anytime it
     * is forced to take focus. This surfaces when the user clicks on a
     * hyperlink to open a Java file; the ApplicationWindow will tell the
     * previously focused control to force focus, and the buffer jumps to the
     * top. The workaround is not make the FormText not focusable, and to
     * achieve this we instantiate/set it manually. This workaround has the
     * drawback that it can't receive keyboard events, eg ctrl-c for copy
     * 
     */
    scrolledFormDetailsText.setFormText(new FormText(scrolledFormDetailsText,
        SWT.NO_FOCUS));
    scrolledFormDetailsText.setBackground(backgroundColor);

    Menu menu = scrolledFormDetailsText.getContent().getMenu();
    MenuItem copyAllMenuItem = new MenuItem(menu, SWT.PUSH);
    copyAllMenuItem.setText("Copy &All");
    copyAllMenuItem.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        copyToClipboard(scrolledFormDetailsTextContents);
      }
    });

    FormText formText = scrolledFormDetailsText.getFormText();
    // Don't collapse consecutive whitespace into a single space
    formText.setWhitespaceNormalized(false);

    formText.addHyperlinkListener(new IHyperlinkListener() {

      public void linkActivated(HyperlinkEvent e) {
        String url = (String) e.getHref();
        if (url.startsWith(JAVA_SOURCE_URL_PREFIX)) {
          openJavaSource(url);
        } else {
          openInDefaultBrowser(url);
        }
      }

      public void linkEntered(HyperlinkEvent e) {
        // Ignore
      }

      public void linkExited(HyperlinkEvent e) {
        // Ignore
      }
    });
  }

  private String createTabString(int n) {
    if (n == 0) {
      return "";
    }
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < n; i++) {
      b.append("\t");
    }
    return b.toString();
  }

  @SuppressWarnings("deprecation")
  private void createViewer(Composite parent) {

    logEntries = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.BORDER, new PatternFilter());

    treeViewer = logEntries.getViewer();
    treeViewer.setComparator(new ViewerComparator() {
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        assert (e1 instanceof LogEntry<?>);
        assert (e2 instanceof LogEntry<?>);

        LogEntry<?> entry1 = (LogEntry<?>) e1;
        LogEntry<?> entry2 = (LogEntry<?>) e2;

        assert (entry2.getParent() == entry1.getParent());
        List<?> siblings = entry1.getParent().getAllChildren();
        return (siblings.indexOf(entry1) - siblings.indexOf(entry2));
      }
    });

    logLabelProvider = new LogLabelProvider<T>();
    treeViewer.setLabelProvider(logLabelProvider);
    treeViewer.setContentProvider(new LogContentProvider<T>());
    treeViewer.setInput(log.getRootLogEntry());
    treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateDetailsPane(event);
      }
    });
    treeViewer.getTree().addKeyListener(
        new EnterKeyTreeToggleKeyAdapter(treeViewer));

    Menu menu = new Menu(treeViewer.getTree());
    MenuItem copy = new MenuItem(menu, SWT.NONE);
    copy.setText("Copy");
    copy.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        copyTreeSelectionToClipboard();
      }
    });

    new MenuItem(menu, SWT.SEPARATOR);

    MenuItem collapseAll = new MenuItem(menu, SWT.NONE);
    collapseAll.setText("Collapse All");
    collapseAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        treeViewer.collapseAll();
      }
    });

    MenuItem expandAll = new MenuItem(menu, SWT.NONE);
    expandAll.setText("Expand All");
    expandAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        treeViewer.expandAll();
      }
    });

    treeViewer.getTree().setMenu(menu);

    treeViewer.getTree().addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        // SWT.MOD1 corresponds to the ctrl key on Windows/linux, and command on
        // mac
        if ((e.stateMask & SWT.MOD1) > 0 && e.keyCode == 'c') {
          copyTreeSelectionToClipboard();
        }
      }
    });

  }

  /**
   * Find the TextConsole associated with the launch. This is required by the
   * {@link JavaStackTraceHyperlink} class (which we subclass).
   */
  private TextConsole getLaunchConsole() {
    LaunchConfiguration launchConfiguration = null;
    T entity = log.getEntity();

    if (entity instanceof BrowserTab) {
      BrowserTab browserTab = (BrowserTab) entity;
      launchConfiguration = browserTab.getLaunchConfiguration();
    } else if (entity instanceof LaunchConfiguration) {
      launchConfiguration = (LaunchConfiguration) entity;
    }

    if (launchConfiguration != null) {
      IProcess[] processes = launchConfiguration.getLaunch().getProcesses();
      if (processes.length > 0) {
        /*
         * Just get the console for the first process. If there are multiple
         * processes, they will all link back to the same ILaunch (which is what
         * JavaStackTraceHyperlink uses the console for anyway).
         */
        IConsole console = DebugUITools.getConsole(processes[0]);
        if (console instanceof TextConsole) {
          return (TextConsole) console;
        }
      }
    }

    return null;
  }

  private void openInDefaultBrowser(String url) {
    BrowserUtilities.launchBrowserAndHandleExceptions(url);
  }

  private void openJavaSource(String url) {
    TextConsole console = getLaunchConsole();
    if (console != null) {
      new DevModeStackTraceHyperlink(url, console).linkActivated();
    } else {
      MessageDialog.openInformation(getShell(), "Google Eclipse Plugin",
          "Could not find Java source context.");
    }
  }

  /**
   * Reveal all children in the model that require attention.
   */
  private void revealChildrenThatNeedAttention(TreeViewer viewer,
      LogEntry<T> entry) {
    Data logData = entry.getLogData();
    if (logData != null && logData.getNeedsAttention()) {
      viewer.reveal(entry);
    }

    List<LogEntry<T>> disclosedChildren = entry.getDisclosedChildren();
    for (LogEntry<T> logEntry : disclosedChildren) {
      revealChildrenThatNeedAttention(viewer, logEntry);
    }
  }

  private void updateDetailsPane(SelectionChangedEvent event) {
    StructuredSelection structuredSelection = (StructuredSelection) event.getSelection();
    if (structuredSelection == null || structuredSelection.isEmpty()) {
      scrolledFormDetailsText.setText("");
      return;
    }

    LogEntry<?> logEntry = (LogEntry<?>) (structuredSelection.getFirstElement());
    Color foregroundColor = logLabelProvider.getForeground(logEntry);
    scrolledFormDetailsText.setForeground(foregroundColor);

    LogEntry.Data data = logEntry.getLogData();

    StringBuffer buf = new StringBuffer();
    buf.append("<p>");

    Date logEntryDate = new Date(data.getTimestamp());
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    buf.append(simpleDateFormat.format(logEntryDate));

    // Add the log level
    // FIXME: Need coloring here, for error entries.
    buf.append(" [");
    buf.append(logEntry.getLogData().getLogLevel());
    buf.append("]");

    // Add the module name
    buf.append(" [");
    buf.append(logEntry.getModuleHandle().getName());
    buf.append("] ");

    buf.append(buildLabelHtml(logEntry));
    buf.append("<br/>");

    // Add the detailed information, if available
    String details = buildDetailsHtml(logEntry.getLogData().getDetails());
    if (details.length() > 0) {
      buf.append("<br/>");
      buf.append(details);
      buf.append("<br/>");
    }

    /*
     * Add help information, if available.
     */
    String helpInfoURL = logEntry.getLogData().getHelpInfoURL();
    if (helpInfoURL.length() > 0) {
      String escapedHelpInfoURL = convertToHtmlContent(helpInfoURL);
      buf.append("<br/>");
      buf.append("See the following URL for additional information:<br/>");
      buf.append("<br/>");
      buf.append("<a href='");
      buf.append(escapedHelpInfoURL);
      buf.append("'>");
      buf.append(escapedHelpInfoURL);
      buf.append("</a>");
      buf.append("<br/>");
    } else {
      /*
       * TODO: We always defer to the Help Info URL, as the Help Info text is in
       * HTML, and we're not rendering HTML in this region as yet.
       */
    }
    buf.append("</p>");
    String text = buf.toString();
    scrolledFormDetailsTextContents = collapseHtmlFormatting(text);
    scrolledFormDetailsText.setText(text);
  }
}
