package com.google.gdt.eclipse.swtbot.conditions;

import com.google.gdt.eclipse.swtbot.AbstractedWaitCondition;
import com.google.gdt.eclipse.swtbot.SwtBotUtils;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.MenuFinder;
import org.hamcrest.Matcher;

import java.util.List;

public class ActiveShellMenu extends AbstractedWaitCondition {

  protected SWTBot bot;
  private String name;
  private boolean recursive;
  private List<MenuItem> found;

  public ActiveShellMenu(SWTBot bot, String name, boolean recursive) {
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
    SwtBotUtils.print("Getting menus for shell: " + bot.activeShell().getText());
    SwtBotUtils.print("Is active: " + bot.activeShell().isActive() + "");

    Matcher<MenuItem> menuMatcher = WidgetMatcherFactory.withMnemonic(name);
    Shell shell = bot.activeShell().widget;
    found = finder.findMenus(shell, menuMatcher, recursive);

    boolean hasFound = found != null && found.size() > 0;
    SwtBotUtils.print("Has found menus: " + hasFound + " for: " + name);
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
    return "Failed to find menus for " + name + " in shell: " + bot.activeShell().getText();
  }

}
