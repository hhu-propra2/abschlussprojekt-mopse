package mops.persistence.directory;

import lombok.*;
import mops.util.AggregateRoot;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;

/**
 * Represents a directory where files can be stored.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@AggregateRoot
public class Directory {

    /**
     * Name Comparator.
     */
    public static final Comparator<Directory> NAME_COMPARATOR = Comparator.comparing(Directory::getName);

    /**
     * Database Id.
     */
    @Id
    @Setter(AccessLevel.PRIVATE)
    private Long id;
    /**
     * Directory name.
     */
    @NonNull
    private String name;
    /**
     * Id of the Directory above this one.
     */
    private Long parentId;
    /**
     * Id of the group which this Directory belongs to.
     */
    private long groupOwner;
    /**
     * Id of the DirectoryPermissions object which stores the access permission for this Directory tree.
     */
    private long permissionsId;
    /**
     * Creation Time.
     */
    @Setter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Exclude
    @CreatedDate
    private Timestamp creationTime;
    /**
     * Last Modified Time.
     */
    @Setter(AccessLevel.PRIVATE)
    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    private Timestamp lastModifiedTime;

    /**
     * Get the creation time.
     *
     * @return creation time
     */
    public Instant getCreationTime() {
        return creationTime == null ? Instant.EPOCH : creationTime.toInstant();
    }

    /**
     * Get the last modified time.
     *
     * @return last modified time
     */
    public Instant getLastModifiedTime() {
        return lastModifiedTime == null ? Instant.EPOCH : lastModifiedTime.toInstant();
    }

    /**
     * Checks whether this directory is a root directory.
     *
     * @return true if this a root directory
     */
    public boolean isRoot() {
        return parentId == null;
    }

    /**
     * Gives you DirectoryBuilder.
     *
     * @return DirectoryBuilder
     */
    public static DirectoryBuilder builder() {
        return new DirectoryBuilder();
    }
}
