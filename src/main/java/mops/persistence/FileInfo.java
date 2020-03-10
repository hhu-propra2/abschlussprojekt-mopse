package mops.persistence;

import java.time.LocalDateTime;

/**
 * Represents a 'file' of the file server.
 */
public interface FileInfo {
   /**
    * @return file id
    */
   long getId();

   /**
    * @return display name
    */
   String getFileName();

   /**
    * @return creation date
    */
   LocalDateTime getCreationDate();
   /**
    * @return Returns content type (pdf, png, etc.) of the file.
    */

   String getContentType();
   /**
    * @return Returns byte size of the file.
    */
   int getSize();
}
