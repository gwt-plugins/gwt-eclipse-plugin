public class GwtWtpMavenBuild extends AbstractGWTBuild
{
    public GwtWtpMavenBuild()
    {
        super("com.gwtplugins.gwt.eclipse.wtp.maven");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.gwtplugins.gdt.eclipse.core");
        addSiblingJar("com.gwtplugins.gwt.eclipse.core");
        addSiblingJar("com.gwtplugins.gdt.eclipse.platform");
        addSiblingJar("com.gwtplugins.gwt.eclipse.oophm");
        addSiblingJar("com.gwtplugins.gwt.eclipse.wtp_");
        
        super.compile();
    }

}
