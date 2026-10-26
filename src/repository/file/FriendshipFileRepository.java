
package repository.file;

import domain.Friendship;
import domain.FriendshipId;
import repository.AbstractFileRepository;
import repository.FriendshipRepository;
import validation.Validator;

import java.time.LocalDateTime;

public class FriendshipFileRepository
        extends AbstractFileRepository<FriendshipId, Friendship>
        implements FriendshipRepository {

    public FriendshipFileRepository(
            String filename,
            Validator<Friendship> validator) {
        super(filename, validator);
        loadFromFile();
    }

    @Override
    protected Friendship extractEntity(String line) {
        String[] fields = line.split(";", -1);

        if (fields.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid friendship format! Expected: id1;id2;createdAt"
            );
        }

        Long id1 = Long.parseLong(fields[0].trim());
        Long id2 = Long.parseLong(fields[1].trim());
        LocalDateTime createdAt =
                LocalDateTime.parse(fields[2].trim());

        return new Friendship(id1, id2, createdAt);
    }

    @Override
    protected String createEntityAsString(Friendship entity) {
        return entity.getId().firstId() + ";" +
                entity.getId().secondId() + ";" +
                entity.getCreatedAt();
    }
}
