package io.cockroachdb.pool.proxy.sandbox.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public abstract class AbstractShellComponent {
    @Autowired
    protected ApplicationContext applicationContext;
}
