package annot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to mark stateless (immutable) classes.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Stateless {
    
}
