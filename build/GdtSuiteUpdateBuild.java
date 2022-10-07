public class GdtSuiteUpdateBuild extends AbstractGWTBuild
{
    public GdtSuiteUpdateBuild()
    {
        super("com.gwtplugins.gdt.eclipse.suite.update");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.suite_");
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");

        super.compile();
    }

}
