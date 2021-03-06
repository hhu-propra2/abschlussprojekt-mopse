package mops.persistence.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * Short meta information strings.
 */
@Data
@AllArgsConstructor
class FileTag {

    /**
     * Tag name.
     */
    @NonNull
    private String name;

}
