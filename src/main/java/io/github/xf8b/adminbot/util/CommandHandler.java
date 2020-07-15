package io.github.xf8b.adminbot.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the class as a command handler. Used for {@link CommandRegistry}.
 *
 * @author xf8b
 * @deprecated use {@link ICommandHandler} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandHandler {
    String name();

    String usage();

    String description();

    String actions() default "";

    String[] aliases() default {};
}
