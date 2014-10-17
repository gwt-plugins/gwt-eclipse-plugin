/**
 *
 */
package com.google.gdt.eclipse.core.sdk;

import java.util.Comparator;

public class SdkSortComparator implements Comparator<Sdk> {

  public enum SortBy {
    VERSION;
  }

  private SortBy sortBy;

  public SdkSortComparator(SortBy sortBy) {
    this.sortBy = sortBy;
  }

  @Override
  public int compare(Sdk left, Sdk right) {
    if (left == null || right == null) {
      return 0;
    }

    int c = 0;
    if (sortBy == SortBy.VERSION) {
      c = SdkUtils.compareVersionStrings(left.getVersion(), right.getVersion());
    }

    return c;
  }

}
