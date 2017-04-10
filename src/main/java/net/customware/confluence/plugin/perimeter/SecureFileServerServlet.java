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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.webresource.servlet.PluginResourceDownload;
import com.atlassian.renderer.util.FileTypeUtil;
import com.atlassian.spring.container.ContainerManager;

/**
 *  @since 2005-12-10
 */
public class SecureFileServerServlet extends BaseFileServerServlet {

    public static final String SERVLET_PATH = "/plugins/servlet/perimeter";

    public static final String ATTACHMENTS_URL_PREFIX = "attachments";
    public static final String RESOURCE_URL_PREFIX = "resources";
    public static final String USER_RESOURCE_URL_PREFIX = "userresources";
    public static final String THUMBNAILS_URL_PREFIX = "thumbnails";

    //~ Methods --------------------------------------------------------------------------------------------------------

    @Override
    public void init(ServletConfig servletConfig) throws ServletException
    {
    	addDownloadStrategy(SecureThumbnailDownload.class);
    	addDownloadStrategy(SecureAttachmentDownload.class);
       // addDownloadStrategy(ResourceDownload.class);
        super.init(servletConfig);
    }

	@Override
    public String getDecodedPathInfo( HttpServletRequest httpServletRequest )
	{
		return GeneralUtil.urlDecode( httpServletRequest.getPathInfo() );
	}

    @Override
    public DownloadStrategy instantiateDownloadStrategy(Class<? extends DownloadStrategy> downloadStrategyClass)
    {
        /**
         * When we are in the bootstrap/setup process, we don't have the Spring context available, and so
         * instantiating plugins will not work properly. Since the flags for each language pack (@see LanguageModuleDescriptor)
         * are stored as resources, we need to make the PluginResourceDownload class available, with the PluginManager set as well
         * (from the bootstrap context).
         *
         * So... we return a normal PluginResourceDownload class that the "bootstrapPluginManager" injected from
         * bootstrapContext.xml
         */
        if (!ContainerManager.isContainerSetup() && downloadStrategyClass.equals(PluginResourceDownload.class))
            return getSimplePluginResourceDownload();

        try
        {
            DownloadStrategy strategy = (DownloadStrategy) downloadStrategyClass.newInstance();
            ContainerManager.autowireComponent(strategy);
            return strategy;
        }
        catch (InstantiationException e)
        {
            throw new PerimeterException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new PerimeterException(e);
        }
    }

    @Override
    protected String urlDecode(String url)
    {
        return GeneralUtil.urlDecode(url);
    }

    protected String getContentType(String location)
    {
        return FileTypeUtil.getContentType(location);
    }


    private PluginResourceDownload getSimplePluginResourceDownload()
    {
            PluginResourceDownload download = new PluginResourceDownload();
            //download.setPluginManager(pluginManager);
            return download;
    }
}
