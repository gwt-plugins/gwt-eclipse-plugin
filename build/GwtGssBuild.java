public class GwtGssBuild extends AbstractGWTBuild
{
    public GwtGssBuild()
    {
        super("com.gwtplugins.gwt.eclipse.gss");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        
        super.compile();
    }

}
