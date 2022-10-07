public class GdtSuiteBuild extends AbstractGWTBuild
{
    public GdtSuiteBuild()
    {
        super("com.gwtplugins.gdt.eclipse.suite");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        addSiblingJar("com.gwtplugins.gwt.eclipse.oophm");

        super.compile();
    }

}
