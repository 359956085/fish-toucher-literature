package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.application.ApplicationManager;

import java.util.Objects;

/** Applies manager and UI side effects after a complete settings snapshot is committed. */
public final class NovelReaderRuntimeCoordinator {

    private NovelReaderRuntimeCoordinator() {}

    public static RuntimeSettings capture(NovelReaderSettings settings) {
        return new RuntimeSettings(
                settings.getPluginMode(),
                settings.getUiLanguage(),
                settings.getHotSearchSource(),
                settings.getCarouselIntervalSeconds(),
                settings.getRefreshIntervalMinutes(),
                settings.getXTrendsRegion(),
                settings.getGoogleTrendsGeo(),
                settings.isShowInStatusBar()
        );
    }

    public static void apply(RuntimeSettings previous, RuntimeSettings current) {
        boolean modeChanged = !Objects.equals(previous.mode(), current.mode());
        HotSearchManager hotSearch = HotSearchManager.getInstance();
        IdleCultivationManager cultivation = IdleCultivationManager.getInstance();

        if (NovelReaderSettings.MODE_HOT_SEARCH.equals(current.mode())) {
            cultivation.stop();
            boolean alreadyRunning = hotSearch.isRunning();
            if (!alreadyRunning) {
                hotSearch.start();
            } else {
                if (timingChanged(previous, current)) {
                    hotSearch.applyTimingChanges();
                }
                if (hotSearchInputChanged(previous, current)) {
                    hotSearch.switchSource();
                }
            }
        } else if (NovelReaderSettings.MODE_CULTIVATION.equals(current.mode())) {
            hotSearch.stop();
            cultivation.start();
        } else {
            hotSearch.stop();
            cultivation.stop();
            if (modeChanged) {
                NovelReaderManager.getInstance().loadMostRecentFileIfNeeded();
            }
        }

        boolean languageChanged = !Objects.equals(previous.language(), current.language());
        boolean statusBarChanged = previous.showInStatusBar() != current.showInStatusBar();
        if (modeChanged || languageChanged) {
            ApplicationManager.getApplication().invokeLater(NovelReaderToolWindowFactory::rebuildAllToolWindows);
        }
        if (languageChanged || statusBarChanged) {
            ApplicationManager.getApplication().invokeLater(NovelReaderToolWindowFactory::refreshAllStatusBarWidgets);
        }
    }

    private static boolean timingChanged(RuntimeSettings previous, RuntimeSettings current) {
        return previous.carouselSeconds() != current.carouselSeconds()
                || previous.refreshMinutes() != current.refreshMinutes();
    }

    private static boolean hotSearchInputChanged(RuntimeSettings previous, RuntimeSettings current) {
        if (!Objects.equals(previous.source(), current.source())) {
            return true;
        }
        return ("x".equals(current.source()) && !Objects.equals(previous.xRegion(), current.xRegion()))
                || ("google".equals(current.source()) && !Objects.equals(previous.googleGeo(), current.googleGeo()));
    }

    public record RuntimeSettings(
            String mode,
            String language,
            String source,
            int carouselSeconds,
            int refreshMinutes,
            String xRegion,
            String googleGeo,
            boolean showInStatusBar
    ) {}
}
