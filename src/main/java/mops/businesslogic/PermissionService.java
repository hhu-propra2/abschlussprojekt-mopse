package mops.businesslogic;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * API for GruppenFindung which handles permissions.
 */
@Service
public interface PermissionService {

    /**
     * @param account user credentials
     * @param groupId the id of the group
     * @return the role of the user in that group
     */
    String fetchRoleForUserInGroup(Account account, long groupId);

    /**
     * @param groupId the id of the group
     * @return gets all roles of that group
     */
    Set<String> fetchRolesInGroup(long groupId);

}