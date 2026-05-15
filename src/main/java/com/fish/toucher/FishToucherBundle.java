package com.fish.toucher;

import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class FishToucherBundle extends DynamicBundle {

    private static final String BUNDLE = "messages.FishToucherBundle";
    private static final FishToucherBundle INSTANCE = new FishToucherBundle();
    private static final ResourceBundle.Control MANUAL_LANGUAGE_CONTROL = new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return List.of(locale);
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    private FishToucherBundle() {
        super(BUNDLE);
    }

    @Nls
    public static @NotNull String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            Object @NotNull ... params) {
        String language = getManualLanguage();
        if (!NovelReaderSettings.LANGUAGE_AUTO.equals(language)) {
            String manualMessage = getManualMessage(language, key, params);
            if (manualMessage != null) {
                return manualMessage;
            }
        }
        return INSTANCE.getMessage(key, params);
    }

    private static @NotNull String getManualLanguage() {
        try {
            return NovelReaderSettings.getInstance().getUiLanguage();
        } catch (Throwable ignored) {
            return NovelReaderSettings.LANGUAGE_AUTO;
        }
    }

    private static String getManualMessage(String language, String key, Object... params) {
        try {
            Locale locale = toLocale(language);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    BUNDLE,
                    locale,
                    FishToucherBundle.class.getClassLoader(),
                    MANUAL_LANGUAGE_CONTROL
            );
            String pattern = bundle.getString(key);
            if (params.length == 0) {
                return pattern;
            }
            return new MessageFormat(pattern, locale).format(params);
        } catch (MissingResourceException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Locale toLocale(String language) {
        if ("en".equals(language)) {
            return Locale.ROOT;
        }
        return Locale.forLanguageTag(language);
    }
}
