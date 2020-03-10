package mops.persistence.permission;

import mops.SpringTestContext;
import mops.persistence.DirectoryPermissionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringTestContext
@DataJdbcTest
class DirectoryPermissionsTest {

    @Autowired
    private DirectoryPermissionsRepository repo;

    private DirectoryPermissions perms;

    @BeforeEach
    void setup() {
        DirectoryPermissionEntry e1 = new DirectoryPermissionEntry("admin", true, true, true);
        DirectoryPermissionEntry e2 = new DirectoryPermissionEntry("user", true, false, false);
        this.perms = new DirectoryPermissions(Set.of(e1, e2));
    }

    @Test
    void failCreation() {
        assertThatThrownBy(() -> new DirectoryPermissionEntry(null, false, false, false))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DirectoryPermissions(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void save() {
        DirectoryPermissions saved = repo.save(perms);

        assertThat(saved).isEqualToIgnoringNullFields(perms);
    }

    @Test
    void loadSave() {
        Long id = repo.save(perms).getId();

        Optional<DirectoryPermissions> loaded = repo.findById(id);

        assertThat(loaded).get().isEqualToIgnoringNullFields(perms);
    }

    @Test
    void loadWriteSave() {
        Long id = repo.save(perms).getId();
        DirectoryPermissions loaded = repo.findById(id).orElseThrow();

        DirectoryPermissionEntry e1 = new DirectoryPermissionEntry("admin", true, true, true);
        DirectoryPermissionEntry e2 = new DirectoryPermissionEntry("user", true, true, true);
        loaded.setPermissions(Set.of(e1, e2));
        Long id2 = repo.save(loaded).getId();

        Optional<DirectoryPermissions> loaded2 = repo.findById(id2);

        assertThat(loaded2).get().isEqualTo(loaded);
    }
}