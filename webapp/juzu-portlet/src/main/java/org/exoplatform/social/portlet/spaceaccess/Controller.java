/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.portlet.spaceaccess;

import javax.inject.Inject;

import juzu.Action;
import juzu.Path;
import juzu.Response;
import juzu.View;
import javax.annotation.PreDestroy;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.core.space.SpaceAccessType;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.portlet.spaceaccess.templates.spaceAccessMessage;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS
 * Author : quangpld
 *          quangpld@exoplatform.com
 * Oct 18, 2012
 */
public class Controller {
  
  @Inject @Path("spaceAccessMessage.gtmpl") spaceAccessMessage message;
  @Inject SpaceService spaceService;

  static private final String ALL_SPACE_LINK = "all-spaces";
  
  @View
  public void index() throws Exception {
    PortalRequestContext pcontext = (PortalRequestContext)(WebuiRequestContext.getCurrentInstance());
    Object statusObject = pcontext.getRequest().getSession().getAttribute(SpaceAccessType.ACCESSED_TYPE_KEY);
    Object spacePrettyNameObj = pcontext.getRequest().getSession().getAttribute(SpaceAccessType.ACCESSED_SPACE_PRETTY_NAME_KEY);
    
    
    if (spacePrettyNameObj == null) {
      message.with()
             .status(statusObject != null ? statusObject.toString() : "")
             .spaceDisplayName("")
             .spacePrettyName("")
             .redirectURI(statusObject != null ? Utils.getURI(ALL_SPACE_LINK) : "").render();
      return;
      
    } 
    
    String status = statusObject.toString();
    
    //
    String spacePrettyName = spacePrettyNameObj.toString();
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    String spaceDisplayName = space.getDisplayName();
    
    if ("social.space.access.not-access-wiki-space".equals(status)) {
      
      Object wikiPageObj = pcontext.getRequest().getSession().getAttribute(SpaceAccessType.ACCESSED_SPACE_WIKI_PAGE_KEY);

      pcontext.sendRedirect(getPermanWikiLink(spacePrettyName, wikiPageObj.toString()));
      return;
    } 
    
    //
    message.with()
           .status(status)
           .spaceDisplayName(spaceDisplayName)
           .spacePrettyName(spacePrettyName)
           .redirectURI("")
           .render();

    
  }
  @View
  public void requestedSuccessful(String spacePrettyName) {
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    message.with()
    .status("social.space.access.requested.success")
    .spaceDisplayName(space.getDisplayName())
    .spacePrettyName("")
    .redirectURI("")
    .render();
  }
  
  @Action
  public Response.Redirect accept(String spacePrettyName) {
    String remoteId = Utils.getOwnerRemoteId();
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    spaceService.addMember(space, remoteId);
    return Response.redirect(Utils.getSpaceHomeURL(space));
  }
  
  @Action
  public Response requestToJoin(String spacePrettyName) {
    String remoteId = Utils.getOwnerRemoteId();
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    spaceService.addPendingUser(space, remoteId);
    return Controller_.requestedSuccessful(spacePrettyName);
  }
  
  @Action
  public Response.Redirect refuse(String spacePrettyName) {
    String remoteId = Utils.getOwnerRemoteId();
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    spaceService.removeInvitedUser(space, remoteId);
    return Response.redirect(Utils.getURI(ALL_SPACE_LINK));
  }
  
  @Action
  public Response.Redirect join(String spacePrettyName) {
    String remoteId = Utils.getOwnerRemoteId();
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    spaceService.addMember(space, remoteId);
    return Response.redirect(Utils.getSpaceHomeURL(space));
  }
  
  /**
   * This method is fake to build permanent wiki link.
   * After Permanent Link feature will be finished by ECMS team, 
   * this method will be removed instead of Wiki API.
   * 
   * @param pcontext
   * @return
   */
  private String getPermanWikiLink(String spacePrettyName, String wikiPage) {
    StringBuilder sb = new StringBuilder("wiki/group/spaces/").append(spacePrettyName).append("/").append(wikiPage);
    
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    //nodeURL.setSchemeUse(true);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), sb.toString());
    return nodeURL.setResource(resource).toString(); 
  }
  
  @PreDestroy
  public void cleanSession() {
    PortalRequestContext pcontext = (PortalRequestContext)(WebuiRequestContext.getCurrentInstance());
    pcontext.getRequest().getSession().removeAttribute(SpaceAccessType.ACCESSED_SPACE_PRETTY_NAME_KEY);
    pcontext.getRequest().getSession().removeAttribute(SpaceAccessType.ACCESSED_TYPE_KEY);
  }

}