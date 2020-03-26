package mops.persistence;

import mops.persistence.group.Group;
import mops.util.AuditingDbContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AuditingDbContext
@DataJdbcTest
class GroupRepositoryTest {

    // TODO: implement test for "findByGroupId"

    @Autowired
    GroupRepository groupRepository;

    @Test
    public void saveTest() {
        Group group = Group.builder()
                .groupId(new UUID(0, 1))
                .member("Segelzwerg", "admin")
                .name("Root")
                .build();
        Group save = groupRepository.save(group);

        assertThat(group).isEqualTo(save);
    }

    @Test
    public void findByUserTest() {
        Group group = Group.builder()
                .groupId(new UUID(0, 12))
                .member("Segelzwerg", "admin")
                .name("Root")
                .build();
        groupRepository.save(group);

        List<Group> groups = groupRepository.findByUser("Segelzwerg");

        assertThat(groups).containsExactly(group);
    }

    @Test
    public void findMultipleGroupsTest() {
        Group group1 = Group.builder()
                .id(123L)
                .groupId(new UUID(0, 123))
                .member("Segelzwerg", "admin")
                .member("iTitus", "viewer")
                .name("1")
                .build();

        Group group2 = Group.builder()
                .id(124L)
                .groupId(new UUID(0, 124))
                .member("Segelzwerg", "admin")
                .name("2")
                .build();

        Group group3 = Group.builder()
                .id(125L)
                .groupId(new UUID(0, 125))
                .member("Segelzwerg", "admin")
                .name("3")
                .build();

        Group group4 = Group.builder()
                .id(126L)
                .groupId(new UUID(0, 126))
                .member("iTitus", "admin")
                .name("4")
                .build();

        Group group5 = Group.builder()
                .id(127L)
                .groupId(new UUID(0, 127))
                .member("Jens", "admin")
                .name("5")
                .build();

        List<Group> groups = List.of(group1, group2, group3, group4, group5);

        groupRepository.saveAll(groups);

        List<Group> groupsOfUser1 = groupRepository.findByUser("Segelzwerg");
        List<Group> groupsOfUser2 = groupRepository.findByUser("iTitus");

        assertThat(groupsOfUser1).containsExactlyInAnyOrder(group1, group2, group3);
        assertThat(groupsOfUser2).containsExactlyInAnyOrder(group1, group4);
    }
}
