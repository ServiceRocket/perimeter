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

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.persistence.ContentEntityObjectDao;
import com.atlassian.confluence.event.events.content.attachment.AttachmentViewEvent;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.actions.PermissionCheckInterceptor;
import com.atlassian.confluence.servlet.FileServerServlet;
import com.atlassian.confluence.servlet.download.AttachmentDownload;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.event.EventManager;
import com.atlassian.plugin.servlet.util.LastModifiedHandler;
import com.atlassian.user.User;
import com.opensymphony.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Downloads the secure attachment, if the secure user has permission.
 */
public class SecureAttachmentDownload extends AttachmentDownload {
    protected final Logger log = LoggerFactory.getLogger( getClass() );

    private PermissionManager permissionManager = null;

    private UserAccessor userAccessor = null;

    private AttachmentManager attachmentManager = null;

    private EventManager eventManager = null;

    private ContentEntityObjectDao contentEntityObjectDao = null;

    public static final String ATTACHMENT_PATH = SecureFileServerServlet.SERVLET_PATH + "/"
            + FileServerServlet.ATTACHMENTS_URL_PREFIX;

    @Override
    public boolean matches( String urlPath ) {
        return urlPath.indexOf( ATTACHMENT_PATH ) != -1;
    }

    // This method appears to have been depreciated - Thomas
    protected boolean hasUserPrivilegeForDownload(HttpServletRequest req, ContentEntityObject entity) {
        log.debug( "Checking user privilage for download." );

        SecureIncludeData data = SecureIncludeData.load(req, entity, getPrefix());

        if (data == null) {
            return false;
        }

        User user = null;

        if (isNotBlank(data.getUsername())) {
            user = userAccessor.getUserByName(data.getUsername());
        }

        if (GeneralUtil.isSuperUser(user)) {
            return true;
        }

        return permissionManager.hasPermission(user, Permission.VIEW, entity);
    }

    private String getPrefix() {
        return FileServerServlet.ATTACHMENTS_URL_PREFIX;
    }

    @Override
    protected String getUrlPrefix() {
        return FileServerServlet.ATTACHMENTS_URL_PREFIX;
    }

    public void setPermissionManager( PermissionManager permissionManager ) {
        this.permissionManager = permissionManager;
    }

    public void setUserAccessor(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    public void setAttachmentManager( AttachmentManager attachmentManager ) {
        this.attachmentManager = attachmentManager;
    }

    public void setContentEntityObjectDao( ContentEntityObjectDao contentEntityObjectDao ) {
        this.contentEntityObjectDao = contentEntityObjectDao;
    }

    @Override
    public java.io.InputStream getStreamForDownload( javax.servlet.http.HttpServletRequest httpServletRequest,
            javax.servlet.http.HttpServletResponse httpServletResponse ) throws IOException {

        ContentEntityObject entity = getEntity( httpServletRequest );

        if ( entity == null || inTrash( entity ) ) {
            httpServletResponse.sendError( HttpServletResponse.SC_NOT_FOUND );
            return null;
        }

        // If the user can view the Attachment, serve it.
        if ( hasUserPrivilegeForDownload( httpServletRequest, entity ) ) {
            Attachment attachment = getAttachment( entity, httpServletRequest );

            if ( attachment == null ) {
                httpServletResponse.sendError( HttpServletResponse.SC_NOT_FOUND );
                return null;
            }

            if ( LastModifiedHandler.checkRequest( httpServletRequest, httpServletResponse,
                    getLastModificationDate( attachment ) ) )
                return null;

            InputStream is = getAttachmentStream( httpServletRequest, httpServletResponse, attachment );

            if ( is == null ) {
                httpServletResponse.sendRedirect( httpServletRequest.getContextPath()
                        + "/attachmentnotfound.action?pageId=" + entity.getId() );
                return null;
            }

            // Must set the headers after the getAttachmentStream is called for
            // thumbnails to ensure that the thumbnail
            // was created when viewed for the first time (CONF-6973)
            // setHeadersForAttachment(attachment, httpServletRequest,
            // httpServletResponse);
            setHeadersForAttachment( attachment.getFileName(), attachment.getFileSize(), attachment
                    .getContentType(), httpServletRequest, httpServletResponse );

            generateAttachmentDownloadEvent( attachment );
            return is;
        } else
            handleUnauthorisedDownload( httpServletRequest, httpServletResponse );

        return null;

    }

    protected ContentEntityObject getEntity( HttpServletRequest httpServletRequest ) {
        long objectId = extractObjectId( httpServletRequest, getUrlPrefix() );
        return contentEntityObjectDao.getById( objectId );
    }

    private boolean inTrash( ContentEntityObject ceo ) {
        return ceo.getContentStatus().equals( ContentEntityObject.DELETED );
    }

    protected Attachment getAttachment( ContentEntityObject entity, HttpServletRequest httpServletRequest ) {
        int version = extractVersion( httpServletRequest );
        String fileName = extractAttachmentFileName( httpServletRequest );

        return attachmentManager.getAttachment( entity, fileName, version );
    }

    protected Date getLastModificationDate( Attachment attachment ) {
        return attachment.getLastModificationDate();
    }

    protected InputStream getAttachmentStream( HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse, Attachment attachment ) throws IOException {
        return attachmentManager.getAttachmentData( attachment );
    }

    protected void setHeadersForAttachment( Attachment attachment, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse ) {
        if ( isHtml( attachment ) ) // CONF-389 prevent cross-site scripting
            httpServletResponse.setContentType( "application/x-download" );
        else
            httpServletResponse.setContentType( attachment.getContentType() );

        httpServletResponse.setHeader( "Content-Length", Long.toString( attachment.getFileSize() ) );
        /**
         * function no longer exist
        setFileNameHeader( httpServletResponse, attachment.getFileName(), httpServletRequest
                .getHeader( "User-Agent" ) );
        */
    }

    protected void generateAttachmentDownloadEvent( Attachment attachment ) {
        eventManager.publishEvent( new AttachmentViewEvent( this, attachment ) );
    }

    private void handleUnauthorisedDownload( HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse ) throws IOException {
        try {
            if ( httpServletRequest.getRemoteUser() == null )
                httpServletRequest.getRequestDispatcher(
                        "/" + PermissionCheckInterceptor.NOT_PERMITTED + ".action" ).forward( httpServletRequest,
                        httpServletResponse );
            else
                httpServletResponse.sendError( HttpServletResponse.SC_NOT_FOUND );
        } catch ( ServletException e ) {
            throw new RuntimeException( "There was a problem serving the attachment:", e );
        }
    }

    protected long extractObjectId( HttpServletRequest httpServletRequest, String prefix ) {
        String[] parts = httpServletRequest.getRequestURI().split( FileServerServlet.PATH_SEPARATOR );
        for ( int i = 0; i < parts.length; i++ ) {
            String part = parts[i];
            if ( part.equals( prefix ) && ( i + 1 ) < parts.length ) {
                return Long.parseLong( parts[i + 1] );
            }
        }

        return -1;
    }

    protected int extractVersion( HttpServletRequest httpServletRequest ) {
        String versionStr = httpServletRequest.getParameter( "version" );
        if ( versionStr != null ) {
            Integer version = GeneralUtil.convertToInteger( versionStr );
            if ( version == null ) {
                throw new RuntimeException( "version string '" + versionStr + "' is not a valid number" );
            }
            return version.intValue();
        }

        return 0;
    }

    protected String extractAttachmentFileName( HttpServletRequest httpServletRequest ) {
        String requestURI = httpServletRequest.getRequestURI();
        requestURI = requestURI.substring( requestURI.lastIndexOf( '/' ) + 1 );
        return GeneralUtil.urlDecode( requestURI );
    }

    private boolean isHtml( Attachment attachment ) {
        String filename = attachment.getFileName();
        boolean isHtmlFileExtension = ( TextUtils.stringSet( filename ) && ( filename.endsWith( "htm" ) || filename
                .endsWith( "html" ) ) );
        boolean isHtmlContentType = "text/html".equals( attachment.getContentType() );
        return isHtmlFileExtension || isHtmlContentType;
    }

    public void setEventManager( EventManager eventManager ) {
        this.eventManager = eventManager;
    }
}
