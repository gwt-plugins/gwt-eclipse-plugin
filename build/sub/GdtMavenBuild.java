package sub;
public class GdtMavenBuild extends AbstractGWTBuild
{
    public GdtMavenBuild()
    {
        super("com.gwtplugins.gdt.eclipse.maven");
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
