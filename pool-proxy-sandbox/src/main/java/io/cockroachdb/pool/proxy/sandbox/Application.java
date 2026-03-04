package io.cockroachdb.pool.proxy.sandbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.cockroachdb.pool.proxy.sandbox.util.AnsiConsole;

@Configuration
@ConfigurationPropertiesScan(basePackageClasses = Application.class)
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
public class Application {
    private static void printHelpAndExit(Consumer<AnsiConsole> message) {
        try (Terminal terminal = TerminalBuilder.terminal()) {
            AnsiConsole console = new AnsiConsole(terminal);
            console.green("Usage: java -jar sandbox.jar [options] [args...]").nl().nl();
            console.yellow("Options include:").nl();
            {
                console.cyan("--profiles [profile,..]   override spring profiles to activate").nl();
                console.cyan("--help                    this help").nl();
            }
            console.nl();
            message.accept(console);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThroughArgs = new LinkedList<>();

        Set<String> profiles = new HashSet<>();

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--help")) {
                printHelpAndExit(ansiConsole -> {
                });
            } else if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printHelpAndExit(ansiConsole -> {
                        ansiConsole.red("Expected list of profile names");
                    });
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else {
                if (arg.startsWith("--") || arg.startsWith("@")) {
                    passThroughArgs.add(arg);
                } else {
                    printHelpAndExit(ansiConsole -> {
                        ansiConsole.red("Unknown argument: " + arg).nl().nl();
                    });
                }
            }
        }

        if (!profiles.isEmpty()) {
            System.setProperty("spring.profiles.active", String.join(",", profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[] {}));
    }
}
