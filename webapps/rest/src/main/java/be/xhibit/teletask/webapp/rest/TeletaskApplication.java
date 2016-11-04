package be.xhibit.teletask.webapp.rest;

import be.xhibit.teletask.webapp.rest.component.ComponentResourceSupport;
import be.xhibit.teletask.webapp.rest.ota.OneTimeAccessResource;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines the components of a JAX-RS application and supplies additional metadata.
 */
public class TeletaskApplication extends Application {
    private final Set<Object> singletons = new HashSet<Object>();

    public TeletaskApplication() {
        this.singletons.add(new ComponentResourceSupport());
        this.singletons.add(new OneTimeAccessResource());
    }

    /**
     * <p>Get a set of root resource and provider instances. Fields and properties
     * of returned instances are injected with their declared dependencies
     * (see {@link Context}) by the runtime prior to use.
     * <p/>
     * <p>Implementations should warn about and ignore classes that do not
     * conform to the requirements of root resource or provider classes.
     * Implementations should flag an error if the returned set includes
     * more than one instance of the same class. Implementations MUST
     * NOT modify the returned set.</p>
     * <p/>
     * <p>The default implementation returns an empty set.</p>
     *
     * @return a set of root resource and provider instances. Returning null
     * is equivalent to returning an empty set.
     */
    @Override
    public Set<Object> getSingletons() {
        return this.singletons;
    }
}
