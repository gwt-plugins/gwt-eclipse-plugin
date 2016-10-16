package com.google.gdt.eclipse.swtbot.conditions;

import com.google.gdt.eclipse.swtbot.SwtBotUtils;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.MenuFinder;
import org.hamcrest.Matcher;

import java.util.List;

public class ActiveShellMenu extends AbstractedWaitCondition {

  public static List<MenuItem> waitForShellMenuList(SWTBot bot, String name, boolean recursive) {
    return new ActiveShellMenu(bot, name, recursive).getMenus();
  }

  private String name;
  private boolean recursive;
  private List<MenuItem> found;

  protected ActiveShellMenu(SWTBot bot, String name, boolean recursive) {
    super(bot);
    this.name = name;
    this.recursive = recursive;
  }

  protected MenuFinder getMenuFinder() {
    return new MenuFinder();
  }

  @Override
  public boolean test() throws Exception {
    MenuFinder finder = getMenuFinder();
    SwtBotUtils.print("ActiveShellMenu: Getting menus for shell: " + bot.activeShell().getText());
    SwtBotUtils.print("ActiveShellMenu: Is active: " + bot.activeShell().isActive() + "");

    Matcher<MenuItem> menuMatcher = WidgetMatcherFactory.withMnemonic(name);
    Shell shell = bot.activeShell().widget;
    found = finder.findMenus(shell, menuMatcher, recursive);

    boolean hasFound = found != null && found.size() > 0;
    SwtBotUtils.print("ActiveShellMenu: Has found menus: '" + hasFound + "' for: " + name);
    return hasFound;
  }

  public List<MenuItem> getMenus() {
    waitForTest();
    return found;
  }

  @Override
  public void init(SWTBot bot) {
    this.bot = bot;
  }

  @Override
  public String getFailureMessage() {
    return "ActiveShellMenu: Failed to find menus for '" + name + "' in shell: " + bot.activeShell().getText();
  }

}
