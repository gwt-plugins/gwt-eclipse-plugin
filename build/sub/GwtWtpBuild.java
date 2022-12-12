package sub;
public class GwtWtpBuild extends AbstractGWTBuild
{
    public GwtWtpBuild()
    {
        super("com.gwtplugins.gwt.eclipse.wtp");
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
