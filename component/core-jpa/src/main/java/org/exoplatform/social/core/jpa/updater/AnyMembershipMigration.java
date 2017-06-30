package org.exoplatform.social.core.jpa.updater;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import java.util.Collection;

public class AnyMembershipMigration implements CustomTaskChange {
  private static final Log LOG                            = ExoLogger.getLogger(AnyMembershipMigration.class);

  private final int BUFFER = 100;

  @Override
  public void execute(Database database) throws CustomChangeException {
    OrganizationService orgService = CommonsUtils.getService(OrganizationService.class);
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);

    // start the migration
    LOG.info("=== Start Social Membership * migration");
    long startTime = System.currentTimeMillis();
    int count = 0;
    int error = 0;

    try {
      RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
      //
      Collection<Group> groups = orgService.getGroupHandler().getAllGroups();
      LOG.debug("There are {} groups", groups.size());

      for (Group group : groups) {
        if (group.getId().startsWith(SpaceUtils.SPACE_GROUP)) {
          LOG.debug("Migrating group {}", group.getId());

          Space space = spaceService.getSpaceByGroupId(group.getId());
          if (space == null) {
            LOG.debug("ignore {} group, no space found", group.getId());
            continue;
          }

          ListAccess<Membership> memberships = orgService.getMembershipHandler().findAllMembershipsByGroup(group);
          int size = memberships.getSize();
          LOG.debug("There are {} memberships", size);

          int fromId = 0;
          int pageSize = size > BUFFER ? BUFFER : size;

          while (pageSize > 0) {
            for (Membership m : memberships.load(fromId, pageSize)) {
              if (MembershipTypeHandler.ANY_MEMBERSHIP_TYPE.equalsIgnoreCase(m.getMembershipType())) {
                LOG.debug("Start migrating {}", m.toString());
                String username = m.getUserName();

                try {
                  if (!spaceService.isManager(space, username)) {
                    if (!spaceService.isMember(space, username)) {
                      spaceService.addMember(space, username);
                    }
                    spaceService.setManager(space, username, true);
                    count++;
                  }
                } catch (Exception e) {
                  error++;
                  LOG.error("error during migrate membership " + m.getId(), e);
                }
              }
            }
            fromId += pageSize;
            pageSize = size - fromId;
            pageSize = pageSize > BUFFER ? BUFFER : pageSize;

            //flush buffer
            RequestLifeCycle.end();
            RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
          }
        }
      }
    } catch (Exception ex) {
      LOG.error("error during migrate memberships", ex);
      throw new CustomChangeException(ex);
    } finally {
      RequestLifeCycle.end();

      LOG.info("=== End Social Membership * {} memberships migrated successfully, {} errors, in {} miliseconds",
          count, error, System.currentTimeMillis() - startTime);
    }
  }

  @Override
  public String getConfirmationMessage() {
      return "ANY memberships migrated as social space managers";
  }

  @Override
  public void setUp() throws SetupException {

  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {

  }

  @Override
  public ValidationErrors validate(Database database) {
      return null;
  }
}
