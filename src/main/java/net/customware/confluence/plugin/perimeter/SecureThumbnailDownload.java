/*
 * Copyright (c) 2007, CustomWare Asia Pacific
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of "CustomWare Asia Pacific" nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.customware.confluence.plugin.perimeter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.servlet.FileServerServlet;
import com.atlassian.confluence.servlet.download.ThumbnailDownload;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.user.User;

/**
 * Provides the download for attachments on a 'secure-include' macro.
 * It will only authorise access to attachments which are:
 * a) attached to the included page and
 * b) visible to the user who originally included the page.
 */
public class SecureThumbnailDownload extends ThumbnailDownload
{
    protected final Logger log = Logger.getLogger(getClass());


    private static final String THUMBNAIL_PATH = SecureFileServerServlet.SERVLET_PATH + "/" + FileServerServlet.THUMBNAILS_URL_PREFIX;

    @Override
    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(THUMBNAIL_PATH) != -1;
    }

    protected boolean hasUserPrivilegeForDownload(HttpServletRequest req, ContentEntityObject entity)
    {
        log.debug("Checking user privilage for download.");

        SecureIncludeData data = SecureIncludeData.load(req, entity, getPrefix());

        if (data == null)
            return false;

        if (data.getContentId() != entity.getId())
            return false;

        String username = data.getUsername();

        User user = null;

        //if (TextUtils.stringSet(username))
        //    user = getUserAccessor().getUser(username);

        if (GeneralUtil.isSuperUser(user))
            return true;

        return true; // permissionManager.hasPermission(user, Permission.VIEW, entity);
    }

    private String getPrefix()
    {
        return FileServerServlet.THUMBNAILS_URL_PREFIX;
    }
}
