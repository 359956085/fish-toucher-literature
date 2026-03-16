package com.fishtoucher.literature;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class FishToucherBundle extends DynamicBundle {

    private static final String BUNDLE = "messages.FishToucherBundle";
    private static final FishToucherBundle INSTANCE = new FishToucherBundle();

    private FishToucherBundle() {
        super(BUNDLE);
    }

    @Nls
    public static @NotNull String message(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
            Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
