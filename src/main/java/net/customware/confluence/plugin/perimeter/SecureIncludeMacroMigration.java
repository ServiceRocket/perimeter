package net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.macro.xhtml.RichTextMacroMigration;

public class SecureIncludeMacroMigration extends RichTextMacroMigration {
	public SecureIncludeMacroMigration(MacroManager xhtmlMacroManager) {
        super(xhtmlMacroManager);
    }
}