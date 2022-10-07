public class GwtCoreBuild extends AbstractGWTBuild
{
    public GwtCoreBuild()
    {
        super("com.gwtplugins.gwt.eclipse.core");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");

        super.compile();
    }

}
