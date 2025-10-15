package io.cockroachdb.poolproxy.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.cockroachdb.poolproxy.test.shell.util.AnsiConsole;
import io.cockroachdb.poolproxy.test.shell.RuntimeIOException;

@Configuration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class
})
@ComponentScan(basePackageClasses = Main.class)
public class Main {
    private static void printHelpAndExit(Consumer<AnsiConsole> message) {
        try (Terminal terminal = TerminalBuilder.terminal()) {
            AnsiConsole console = new AnsiConsole(terminal);
            console.green("Usage: java -jar ppt.jar [options] [args...]").nl().nl();
            console.yellow("Options include:").nl();
            {
                console.cyan("--profiles [profile,..]   override spring profiles to activate").nl();
                console.cyan("--help                    this help").nl();
            }
            console.nl();
            message.accept(console);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
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

        if (profiles.isEmpty()) {
            profiles.add("default");
        }

        System.setProperty("spring.profiles.active", String.join(",", profiles));

        new SpringApplicationBuilder(Main.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[]{}));
    }
}
