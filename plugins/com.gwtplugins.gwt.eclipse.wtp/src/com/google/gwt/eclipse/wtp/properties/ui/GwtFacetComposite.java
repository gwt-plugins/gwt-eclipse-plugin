package com.google.gwt.eclipse.wtp.properties.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class GwtFacetComposite extends Composite {

  private Button btnAutoStartThe;

  /**
   * Create the composite.
   * 
   * @param parent
   * @param style
   */
  public GwtFacetComposite(Composite parent, int style) {
    super(parent, style);
    setLayout(new FillLayout(SWT.HORIZONTAL));

    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new GridLayout(1, false));

    Label lblGwtFacet = new Label(composite, SWT.NONE);
    lblGwtFacet.setText("Super Development Mode");

    btnAutoStartThe = new Button(composite, SWT.CHECK);
    btnAutoStartThe.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
      }
    });
    btnAutoStartThe.setText("Sync the CodeServer running state with the WTP Server.\n"
        + "The WTP Server start and stop will start and stop the CodeServer.");
    btnAutoStartThe.setSelection(true);

    // TODO provide a link to docs for the code server sync
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  public Boolean getSyncServer() {
    return btnAutoStartThe.getSelection();
  }

  public void setSyncServer(Boolean syncServer) {
    btnAutoStartThe.setSelection(syncServer);
  }

}
