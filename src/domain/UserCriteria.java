package domain;

import java.util.function.Predicate;
import java.util.Objects;
/**
 * Provides reusable predicates for filtering users.
 */
public class UserCriteria {
    /**
     * Creates a predicate that matches users by their exact last name.
     * The comparison is case-sensitive.
     *
     * @param lastName the last name to match, or null to match a null last name
     * @return a predicate that tests whether a user has the specified last name
     */
    public static Predicate<User> hasLastName(String lastName)
    {
        return user -> Objects.equals(user.getLastName(), lastName);
    }

    /**
     * Creates a predicate that matches users by their exact first name.
     * The comparison is case-sensitive.
     *
     * @param firstName the first name to match, or null to match a null first name
     * @return a predicate that tests whether a user has the specified first name
     */
    public static Predicate<User> hasFirstName(String firstName)
    {
        return user -> Objects.equals(user.getFirstName(), firstName);
    }

}
