package io.cockroachdb.poolproxy.test.shell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.slf4j.LoggerFactory;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

@Component
public class Prompt implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) LoggerFactory
                .getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("io.cockroachdb");
        int fg = switch (logger.getLevel().toInt()) {
            case Level.TRACE_INT -> AttributedStyle.MAGENTA;
            case Level.DEBUG_INT -> AttributedStyle.CYAN;
            case Level.INFO_INT -> AttributedStyle.GREEN;
            case Level.WARN_INT -> AttributedStyle.YELLOW;
            case Level.ERROR_INT -> AttributedStyle.RED;
            default -> AttributedStyle.GREEN;
        };
        return new AttributedString("poolproxy:$ ",
                AttributedStyle.DEFAULT.foreground(fg));
    }
}
