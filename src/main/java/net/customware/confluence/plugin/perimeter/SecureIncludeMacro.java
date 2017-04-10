/*
 * Copyright (c) 2017, ServiceRocket Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of "ServiceRocket Inc" nor the names of its contributors may
 *       be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.customware.confluence.plugin.perimeter;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ConfluenceEntityObject;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.servlet.FileServerServlet;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.context.StaticHttpContext;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.stream.XMLStreamException;
import java.util.Map;

import static com.atlassian.confluence.util.GeneralUtil.htmlEncode;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @since 2005-12-10
 */
public class SecureIncludeMacro extends BaseMacro implements Macro {
    private static Logger log = getLogger(SecureIncludeMacro.class);
    private static final String ID = "id";
    public static final String ID_PARAM = "secureIncludeId";
    public static final String LINK_PARAM = "secureIncludeLink";
    public static final String PAGE_ID_PARAM = "pageId";

    private ContentEntityManager contentEntityManager;
    private PermissionManager permissionManager;
    private UserAccessor userAccessor;
    private BootstrapManager bootstrapManager;
    private XhtmlContent xhtmlContent;
    private LinkAssistant linkAssistant;

    @Override
    public String execute(Map params, String body, RenderContext renderContext) throws MacroException {
        try {
            return execute(params, body, new DefaultConversionContext(renderContext));
        } catch (MacroExecutionException e) {
            throw new MacroException(e);
        }
    }

    @Override
    public String execute(Map<String, String> params, String body, ConversionContext ctx) throws MacroExecutionException {
        String id = isNotBlank(params.get(ID)) ? params.get(ID) : null;
        if (id == null)
            throw new MacroExecutionException("Please supply an id which is unique to this page.");

        SecureIncludeData data = SecureIncludeData.load(ctx.getEntity(), id);

        if (data == null) {
            data = processRequest(id, ctx);
        }

        if (data != null) {
            return includePage(id, data, ctx);
        } else {
            ConfluenceUser user = AuthenticatedUserThreadLocal.get();
            if (permissionManager.hasPermission(user, Permission.EDIT, ctx.getEntity()))
                return inputForm(id, ctx);

            return "";
        }
    }

    private String inputForm(String id, ConversionContext ctx) {
        String link = getRequestParam(LINK_PARAM, null);
        String ctxPath = ctx.getPageContext().getSiteRoot();

        StringBuffer out = new StringBuffer();

        out.append("<div style='border: 1px dashed gray'>");
        out.append("<form id=\"secureIncludeForm_" + htmlEncode(id) + "\" name=\"secureIncludeForm\" method=\"post\" action=\"")
                .append(ctxPath).append("/pages/viewpage.action?pageId=")
                .append(ctx.getEntity().getId()).append("\">\n");

        out.append("<input type=\"hidden\" name=\"").append(ID_PARAM)
                .append("\" value=\"").append(htmlEncode(id)).append("\"/>\n");

        if (link != null)
            out.append("<div class='error'>The specified link does not exist or is not accessible.</div>\n");

        out.append("<p>");
        out.append("Enter the link to any other page you can view, in any space, and anyone who can view <i>this</i> page will be able to view it.<br/>");
        out.append("<b>Link:</b> <input type='text' name='").append(LINK_PARAM)
                .append("' value=\"").append((link == null) ? "" : link).append("\" width='20'/>");
        out.append(" <input type='submit' name='go' value='Include'/>");
        out.append("</p>\n");

        out.append("</form>");
        out.append("</div>");

        return out.toString();
    }

    private SecureIncludeData processRequest(String id, ConversionContext ctx) {
        if (id.equals(getRequestParam(ID_PARAM, null))) {
            String link = getRequestParam(LINK_PARAM, null);
            if (link != null && link.length() > 0) {
                // Try to find the content.
                ConfluenceEntityObject entity = linkAssistant.getEntityForWikiLink(ctx, link);
                if (entity instanceof ContentEntityObject) {
                    User user = AuthenticatedUserThreadLocal.get();
                    if (permissionManager.hasPermission(user, Permission.VIEW, entity)) {
                        SecureIncludeData data = new SecureIncludeData();
                        data.setUsername(user.getName());
                        data.setContentId(entity.getId());
                        SecureIncludeData.save(ctx.getEntity(), id, data);
                        return data;
                    }
                }
            }
        }
        return null;
    }

    private String getRequestParam(String name, String defaultVal) {
        if ((new StaticHttpContext()).getRequest() != null) {
            String param = (new StaticHttpContext()).getRequest().getParameter(name);
            if (isBlank(param)) {
                return defaultVal;
            } else {
                return param;
            }
        }
        return defaultVal;
    }

    private String includePage(String id, SecureIncludeData data, ConversionContext conversionContext) throws MacroExecutionException {
        ContentEntityObject targetContent = getContentEntityManager().getById(data.getContentId());

        if (targetContent == null) {
            throw new MacroExecutionException("The targetContent this secure include accesses no longer exists.");
        }

        ConfluenceUser user = userAccessor.getUserByName(data.getUsername());
        if (user == null) {
            throw new MacroExecutionException("The user who set up this secure include no longer exists: " + data.getUsername());
        }

        if (!permissionManager.hasPermission(user, Permission.VIEW, targetContent)) {
            throw new MacroExecutionException("The user who set up this secure include no longer has access to the resource.");
        }

        PageContext ctx = targetContent.toPageContext();
        String contentIncludeId = SecureAttachmentDownload.ATTACHMENT_PATH + "/" + targetContent.getId()
                + "/" + conversionContext.getEntity().getId() + "/" + GeneralUtil.urlEncode(id);

        String securePath = bootstrapManager.getWebAppContextPath() + contentIncludeId;

        ctx.setSiteRoot(conversionContext.getPageContext().getSiteRoot());
        ctx.setBaseUrl(conversionContext.getPageContext().getBaseUrl());
        ctx.setImagePath(conversionContext.getPageContext().getImagePath());
        ctx.setAttachmentsPath(securePath);

        // Fake logging in as the original accessor...
        ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();
        AuthenticatedUserThreadLocal.set(user);

        //return subRenderer.render(targetContent.getContent(), ctx, RenderMode.ALL);
        String rendered;
        try {
            rendered = xhtmlContent.convertStorageToView(targetContent.getBodyAsString(), conversionContext);
        } catch (XhtmlException | XMLStreamException e) {
            throw new MacroExecutionException(e.getMessage());
        }

        // Return to the real user...
        AuthenticatedUserThreadLocal.set(currentUser);

        rendered = replaceAttachmentUrls(rendered, data, contentIncludeId);

        return rendered;
    }

    private String replaceAttachmentUrls(String rendered, SecureIncludeData data, String contentIncludeId) {
        String attachmentUrl = "/" + FileServerServlet.SERVLET_PATH + "/" + FileServerServlet.ATTACHMENTS_URL_PREFIX + "/" + data.getContentId();
        return rendered.replaceAll(attachmentUrl, contentIncludeId);
    }

    public boolean isInline() {
        return false;
    }

    public boolean hasBody() {
        return true;
    }

    public RenderMode getBodyRenderMode() {
        return RenderMode.suppress(RenderMode.F_FIRST_PARA);
    }

    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.INLINE;
    }

    public ContentEntityManager getContentEntityManager() {
        if (contentEntityManager == null) {
            contentEntityManager = (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        }
        return contentEntityManager;
    }

    @Autowired
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Autowired
    public void setUserAccessor(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    @Autowired
    public void setBootstrapManager(BootstrapManager bootstrapManager) {
        this.bootstrapManager = bootstrapManager;
    }

    @Autowired
    public void setXhtmlContent(XhtmlContent xhtmlContent) {
        this.xhtmlContent = xhtmlContent;
    }

    @Autowired
    public void setLinkAssistant(LinkAssistant linkAssistant) {
        this.linkAssistant = linkAssistant;
    }
}
