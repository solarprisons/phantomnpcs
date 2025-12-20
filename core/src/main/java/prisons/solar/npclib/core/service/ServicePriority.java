package prisons.solar.npclib.core.service;

/**
 * Standard priority values for service registration.
 * Higher values = higher precedence (selected first).
 */
public final class ServicePriority {

    /**
     * Priority for custom/user implementations (highest precedence)
     * Custom implementations always override built-in ones
     */
    public static final int CUSTOM = 1000;

    /**
     * Priority for built-in/default implementations (fallback)
     * Used when no custom implementation is registered
     */
    public static final int DEFAULT = 0;

    /**
     * Priority for third-party plugin implementations (between custom and default)
     */
    public static final int PLUGIN = 500;

    private ServicePriority() {
        throw new UnsupportedOperationException("Utility class");
    }
}