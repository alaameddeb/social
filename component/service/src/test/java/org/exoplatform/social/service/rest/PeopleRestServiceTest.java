/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.service.rest;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import junit.framework.AssertionFailedError;

import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.service.test.AbstractResourceTest;

import java.util.ArrayList;

public class PeopleRestServiceTest  extends AbstractResourceTest {
  static private PeopleRestService peopleRestService;
  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private ActivityStorageImpl activityStorage;


  public void setUp() throws Exception {
    super.setUp();
    peopleRestService = new PeopleRestService();
    identityManager =  (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityStorage = (ActivityStorageImpl) getContainer().getComponentInstanceOfType(ActivityStorageImpl.class);
    relationshipManager =  (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
    registry(peopleRestService);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    unregistry(peopleRestService);
  }

  public void testSuggestUsernames() throws Exception {
    MultivaluedMap<String, String> h = new MultivaluedMapImpl();
    String username = "root";
    h.putSingle("username", username);
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service("GET", "/social/people/suggest.json?nameToSearch=R&currentUser=root", "", h, null, writer);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals("application/json;charset=utf-8", response.getContentType().toString());
    if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode())
      throw new AssertionFailedError("Service not found");
  }

    public void testUserMentionInComment() throws Exception {
    //Given
    Identity rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    Identity demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    Identity maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    Identity johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", true);
    final String TITLE = "activity on root stream";
    ExoSocialActivity demoActivity = new ExoSocialActivityImpl();
    demoActivity.setTitle(TITLE);
    activityStorage.saveActivity(demoIdentity, demoActivity);
    Relationship relationship = new Relationship(rootIdentity, maryIdentity);
    relationship.setStatus(Relationship.Type.CONFIRMED);
    relationshipManager.update(relationship);
    MultivaluedMap<String, String> h2 = new MultivaluedMapImpl();
    String username = "root";
    h2.putSingle("username", username);
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();

    //When
    ContainerResponse response = service("GET", "/social/people/suggest.json?nameToSearch=m&currentUser=root&typeOfRelation=mention_comment&activityId=" + demoActivity.getId(), "", h2, null, writer);

    //Then
    assertEquals(200, response.getStatus());
    assertTrue(((ArrayList) response.getEntity()).size() == 2);


    identityManager.deleteIdentity(rootIdentity);
    identityManager.deleteIdentity(demoIdentity);
    identityManager.deleteIdentity(maryIdentity);
    identityManager.deleteIdentity(johnIdentity);
  }
}
