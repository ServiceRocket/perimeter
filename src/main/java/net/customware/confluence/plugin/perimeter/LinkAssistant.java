package net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ConfluenceEntityObject;

/**
 * @author khailon
 * @since 4.0.0.20160606
 */
public interface LinkAssistant {

    /**
     * Parses a content name (e.g. 'SPACE:Page' or 'Page^attachment.ext') and
     * returns the entity it identifies.
     *
     * @param context
     *            The context to get spaces/content id info from if not
     *            specified in the contentName.
     * @param linkText
     *            The name of the entity being tracked
     * @return The entity matching the name, or <code>null</code> if it could
     *         not be found.
     */
    ConfluenceEntityObject getEntityForWikiLink(ConversionContext context, String linkText );
}
