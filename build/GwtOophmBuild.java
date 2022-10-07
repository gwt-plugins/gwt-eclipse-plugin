public class GwtOophmBuild extends AbstractGWTBuild
{
    public GwtOophmBuild()
    {
        super("com.gwtplugins.gwt.eclipse.oophm");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");

        super.compile();
    }

}
