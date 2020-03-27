package mops.businesslogic.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mops.businesslogic.exception.DatabaseException;
import mops.businesslogic.security.Account;
import mops.exception.MopsException;
import mops.persistence.GroupRepository;
import mops.persistence.group.Group;
import mops.persistence.permission.DirectoryPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * {@inheritDoc}
 * This is used during production.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    /**
     * Represents the role of an admin.
     */
    @Value("${material1.mops.configuration.role.admin}")
    @SuppressWarnings({ "PMD.ImmutableField", "PMD.BeanMembersShouldSerialize" })
    private String adminRole = "admin";
    /**
     * Represents the role of a viewer.
     */
    @Value("${material1.mops.configuration.role.viewer}")
    @SuppressWarnings({ "PMD.ImmutableField", "PMD.BeanMembersShouldSerialize" })
    private String viewerRole = "viewer";

    /**
     * Access to our Group Database.
     */
    private final GroupRepository groupRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter") // stream
    public Set<String> getRoles(long groupId) throws MopsException {
        return Set.of(adminRole, viewerRole);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter" }) // iterable fluent api
    public List<Group> getAllGroups() throws MopsException {
        List<Group> groups = new ArrayList<>();
        try {
            groupRepository.findAll().forEach(groups::add);
        } catch (Exception e) {
            log.error("Error while getting all groups:", e);
            throw new DatabaseException("Konnte nicht alle Gruppen laden.", e);
        }
        return groups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public List<Group> getUserGroups(Account account) throws MopsException {
        try {
            return groupRepository.findByUser(account.getName());
        } catch (Exception e) {
            log.error("Error while getting all groups for user {}:", account.getName(), e);
            throw new DatabaseException("Konnte nicht alle Gruppen eines Benutzers laden.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter" }) //builder
    public DirectoryPermissions getDefaultPermissions(long groupId) {
        return DirectoryPermissions.builder()
                .entry(adminRole, true, true, true)
                .entry(viewerRole, true, false, false)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AvoidCatchingGenericException" })
    public Group getGroup(long groupId) throws MopsException {
        try {
            return groupRepository.findById(groupId).orElseThrow();
        } catch (Exception e) {
            log.error("Failed to retrieve group with id '{}':", groupId, e);
            throw new DatabaseException(
                    "Die Gruppe konnte nicht gefunden werden, bitte versuchen sie es später nochmal!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AvoidCatchingGenericException" })
    public Optional<Group> findGroupByGroupId(UUID groupId) throws MopsException {
        try {
            return groupRepository.findByGroupId(groupId);
        } catch (Exception e) {
            log.error("Failed to retrieve group with group uuid '{}':", groupId, e);
            throw new DatabaseException(
                    "Die Gruppe konnte nicht gefunden werden, bitte versuchen sie es später nochmal!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AvoidCatchingGenericException" })
    public Group saveGroup(Group group) throws MopsException {
        try {
            return groupRepository.save(group);
        } catch (Exception e) {
            log.error("Failed to save group '{}' with group uuid '{}':", group.getName(), group.getGroupId(), e);
            throw new DatabaseException("Die Gruppe konnte nicht gespeichert werden!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AvoidCatchingGenericException" })
    public void deleteGroup(long groupId) throws MopsException {
        try {
            groupRepository.deleteById(groupId);
        } catch (Exception e) {
            log.error("Failed to delete group with id '{}':", groupId, e);
            throw new DatabaseException("Die Gruppe konnte nicht gelöscht werden!", e);
        }
    }
}