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

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContentPropertyManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.servlet.FileServerServlet;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.thoughtworks.xstream.XStream;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: david
 * Date: Dec 10, 2005
 * Time: 1:54:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class SecureIncludeData implements Serializable
{
    private static final String SECURE_INCLUDE_PREFIX = "org.randombits.confluence.perimeter.SecureInclude:";

    private String username;
    private long contentId;
    private static ContentEntityManager contentEntityManager;
    private static PermissionManager permissionManager;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public long getContentId()
    {
        return contentId;
    }

    public void setContentId(long contentId)
    {
        this.contentId = contentId;
    }

    private static XStream createXStream()
    {
        XStream xStream = new XStream();
        xStream.setClassLoader(SecureIncludeData.class.getClassLoader());

        xStream.alias("secure-include-data", SecureIncludeData.class);

        return xStream;
    }

    public static void save(ContentEntityObject content, String id, SecureIncludeData data)
    {
        XStream xStream = createXStream();
        ContentPropertyManager cpm = (ContentPropertyManager) ContainerManager.getComponent("contentPropertyManager");

        String dataXml = xStream.toXML(data);
        cpm.setTextProperty(content, makeKey(id), dataXml);
    }

    public static SecureIncludeData load(ContentEntityObject content, String id)
    {
        XStream xStream = createXStream();
        ContentPropertyManager cpm = (ContentPropertyManager) ContainerManager.getComponent("contentPropertyManager");

        String dataXml = cpm.getTextProperty(content, makeKey(id));
        if (dataXml != null)
            return (SecureIncludeData) xStream.fromXML(dataXml);

        return null;
    }

    private static String makeKey(String id)
    {
        return SECURE_INCLUDE_PREFIX + id;
    }

    public static SecureIncludeData load(HttpServletRequest req, ContentEntityObject entity, String prefix)
    {
        long sourceId = extractSourceId(req, prefix);
        String includeId = extractIncludeId(req, prefix);

        if (includeId == null || sourceId == 0L)
            return null;

        ContentEntityObject source = getContentEntityManager().getById(sourceId);
        User currentUser = AuthenticatedUserThreadLocal.getUser();
        if (!getPermissionManager().hasPermission(currentUser, Permission.VIEW, source))
            return null;

        SecureIncludeData data = load(source, includeId);

        if (data.getContentId() != entity.getId())
            return null;

        return data;
    }

    private static PermissionManager getPermissionManager()
    {
        if (permissionManager == null)
            permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");
        return permissionManager;
    }

    private static ContentEntityManager getContentEntityManager()
    {
        if (contentEntityManager == null)
            contentEntityManager = (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        return contentEntityManager;
    }

    static long extractSourceId(HttpServletRequest httpServletRequest, String prefix)
    {
        String[] parts = httpServletRequest.getRequestURI().split(FileServerServlet.PATH_SEPARATOR);
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.equals(prefix) && (i + 1) < parts.length)
            {
                return Long.parseLong(parts[i + 2]);
            }
        }

        return -1;
    }

    static String extractIncludeId(HttpServletRequest httpServletRequest, String prefix)
    {
        String[] parts = httpServletRequest.getRequestURI().split(FileServerServlet.PATH_SEPARATOR);
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.equals(prefix) && (i + 1) < parts.length)
            {
                return parts[i + 3];
            }
        }

        return null;
    }
}
