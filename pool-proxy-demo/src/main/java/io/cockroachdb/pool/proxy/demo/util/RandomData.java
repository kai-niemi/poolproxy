package io.cockroachdb.pool.proxy.demo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class RandomData {
    private static final Logger logger = LoggerFactory.getLogger(RandomData.class);

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static final List<String> firstNames = new ArrayList<>();

    private static final List<String> lastNames = new ArrayList<>();

    private static final List<String> cities = new ArrayList<>();

    private static final List<String> countries = new ArrayList<>();

    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    static {
        firstNames.addAll(readLines("random/firstname_female.txt"));
        firstNames.addAll(readLines("random/firstname_male.txt"));
        lastNames.addAll(readLines(("random/surnames.txt")));
        cities.addAll(readLines(("random/cities.txt")));

        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.hasLength(locale.getDisplayCountry(Locale.US))) {
                countries.add(locale.getDisplayCountry(Locale.US));
            }
        }
    }

    private static List<String> readLines(String path) {
        try (InputStream resource = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
        }
        return Collections.emptyList();
    }

    public static BigDecimal randomMoneyBetween(double low, double high, int fractions) {
        if (high <= low) {
            throw new IllegalArgumentException("high<=low");
        }
        return BigDecimal.valueOf(Math.max(low, random.nextDouble() * high))
                .setScale(fractions, RoundingMode.HALF_UP);
    }

    public static <E> E selectRandom(List<E> collection) {
        return collection.get(random.nextInt(collection.size()));
    }

    public static <E> E selectRandom(E[] collection) {
        return collection[random.nextInt(collection.length)];
    }

    public static int randomInt(int start, int end) {
        return random.nextInt(start, end);
    }

    public static String randomFirstName() {
        return selectRandom(firstNames);
    }

    public static String randomLastName() {
        return selectRandom(lastNames);
    }

    public static String randomCity() {
        return StringUtils.capitalize(selectRandom(cities));
    }

    public static String randomCountry() {
        return selectRandom(countries);
    }

    public static String randomZipCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomWord(int min) {
        byte[] buffer = new byte[min];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}

