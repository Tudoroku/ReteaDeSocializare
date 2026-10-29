package repository;

/** Storage choices accepted, with exactly this capitalization, by the factory. */
public enum RepositoryType {
    /** Stores entity references only in memory, without automatic domain validation. */
    MEMORY,
    /** Loads and rewrites validated entities in UTF-8 text files. */
    FILE,
    /** Selects database placeholders whose repository operations are unsupported. */
    DATABASE
}
