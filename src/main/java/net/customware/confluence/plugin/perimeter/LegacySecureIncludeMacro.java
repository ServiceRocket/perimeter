package net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;

import java.util.Map;
@Deprecated
public class LegacySecureIncludeMacro extends BaseMacro {

    private SecureIncludeMacro newMacro;
    
    private static final RenderMode RENDER_MODE = RenderMode.suppress(RenderMode.F_FIRST_PARA);

    public LegacySecureIncludeMacro() {
        newMacro = new SecureIncludeMacro();
    }

    public RenderMode getBodyRenderMode() {
        return RENDER_MODE;
    }

    @Override
    public String execute(Map map, String s, RenderContext renderContext) throws MacroException {
        try {
            return newMacro.execute(map, s, new DefaultConversionContext(renderContext));
        } catch (MacroExecutionException e) {
            throw new MacroException(e);
        }
    }

    public boolean hasBody() {
        return true;
    }

}