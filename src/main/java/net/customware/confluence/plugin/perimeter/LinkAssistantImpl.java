package net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ConfluenceEntityObject;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * @author khailon
 * @since 4.0.0.20160606
 */
@Component
public class LinkAssistantImpl implements LinkAssistant {

    private enum LinkPart {
        ANCHOR,
        SPACE_KEY,
        ATTACHMENT_NAME
    }

    private final static char SPACE_SEPARATOR = ':';
    private final static char ATTACHMENT_SEPARATOR = '^';
    private final static char ANCHOR_SEPARATOR = '#';
    private final static char ID_PREFIX = '$';

    private PermissionManager permissionManager;
    private AttachmentManager attachmentManager;
    private PageManager pageManager;

    @Override
    public ConfluenceEntityObject getEntityForWikiLink(ConversionContext context, String linkText) {
        ConfluenceEntityObject content = findEntityForWikiLink(context.getSpaceKey(), linkText, EnumSet.noneOf(LinkAssistantImpl.LinkPart.class));

        if (content != null) {
            // Check that the current user is allowed to view the target entity
            User user = AuthenticatedUserThreadLocal.get();
            return permissionManager.hasPermission(user, Permission.VIEW, content) ? content : null;
        }

        return null;
    }

    private ConfluenceEntityObject findEntityForWikiLink(String spaceKey, String linkText, EnumSet<LinkAssistantImpl.LinkPart> processed) {
        ConfluenceEntityObject content = null;

        // Then, we try the title as is
        if (AbstractPage.isValidPageTitle(linkText)) {
            content = getPageManager().getPage(spaceKey, linkText);
        }

        // Then we check if it's an absolute ID
        if (content == null && linkText.startsWith(String.valueOf(ID_PREFIX))) {
            try {
                long id = Long.parseLong(linkText.substring(1));
                content = getPageManager().getById(id);
            } catch (NumberFormatException e) {
                // Do nothing.
            }
        }

        // Next, we try checking for a '#'
        if (content == null) {
            if (!processed.contains(LinkAssistantImpl.LinkPart.ANCHOR)) {
                int lastAnchor = linkText.lastIndexOf(ANCHOR_SEPARATOR);
                if (lastAnchor >= 0) {
                    content = findEntityForWikiLink(spaceKey, linkText.substring(0, lastAnchor), addPart(processed, LinkAssistantImpl.LinkPart.ANCHOR));
                }
            }

            if (content == null) {
                if (!processed.contains(LinkAssistantImpl.LinkPart.ATTACHMENT_NAME)) {
                    int lastAttachment = linkText.lastIndexOf(ATTACHMENT_SEPARATOR);
                    if (lastAttachment >= 0) {
                        String pageTitle = linkText.substring(0, lastAttachment);
                        String attachmentName = linkText.substring(lastAttachment + 1);
                        ConfluenceEntityObject page = findEntityForWikiLink(spaceKey, pageTitle, addPart(processed, LinkAssistantImpl.LinkPart.ATTACHMENT_NAME));
                        if (page instanceof ContentEntityObject) {
                            content = attachmentManager.getAttachment((ContentEntityObject) page, attachmentName);
                        }
                    }
                }

                if (content == null) {
                    if (!processed.contains(LinkAssistantImpl.LinkPart.SPACE_KEY)) {
                        int firstSpace = linkText.indexOf(SPACE_SEPARATOR);
                        if (firstSpace >= 0) {
                            String space = linkText.substring(0, firstSpace);
                            String pageTitle = linkText.substring(firstSpace + 1);
                            content = findEntityForWikiLink(space, pageTitle, addPart(processed, LinkAssistantImpl.LinkPart.SPACE_KEY));
                        }
                    }
                }
            }
        }
        return content;
    }

    private EnumSet<LinkAssistantImpl.LinkPart> addPart(EnumSet<LinkAssistantImpl.LinkPart> processedParts, LinkAssistantImpl.LinkPart anchor ) {
        EnumSet<LinkAssistantImpl.LinkPart> clone = processedParts.clone();
        clone.add( anchor );
        return clone;
    }

    @Autowired
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Autowired
    public void setAttachmentManager(AttachmentManager attachmentManager) {
        this.attachmentManager = attachmentManager;
    }

    public PageManager getPageManager() {
        if (pageManager == null) {
            pageManager = (PageManager) ContainerManager.getComponent("pageManager");
        }
        return pageManager;
    }
}
