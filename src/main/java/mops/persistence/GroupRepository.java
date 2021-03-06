package mops.persistence;

import mops.persistence.group.Group;
import mops.util.AggregateBuilder;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database connection for groups.
 */
@Repository
@AggregateBuilder
public interface GroupRepository extends CrudRepository<Group, Long> {

    /**
     * Find group by its uuid.
     *
     * @param groupId group id
     * @return optional group
     */
    @Query("SELECT * FROM group_table WHERE group_id =:groupId")
    Optional<Group> findByGroupId(@Param("groupId") UUID groupId);

    /**
     * Get all groups where a given user is a member.
     *
     * @param name user name
     * @return list of groups
     */
    @Query("SELECT * FROM group_table "
            + "INNER JOIN group_member ON group_table.id = group_member.group_id "
            + "WHERE group_member.name = :name")
    List<Group> findByUser(@Param("name") String name);

}
