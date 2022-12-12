package sub;
public class GdtCoreBuild extends AbstractGWTBuild
{
    public GdtCoreBuild()
    {
        super("com.gwtplugins.gdt.eclipse.core");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        super.compile();
    }
}
