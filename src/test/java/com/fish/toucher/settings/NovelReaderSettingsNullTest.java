package com.fish.toucher.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovelReaderSettingsNullTest {

    @Test
    void 空热搜配置应回退默认值() {
        NovelReaderSettings.State state = new NovelReaderSettings.State();
        state.hotSearchSource = null;
        state.xTrendsRegion = null;
        state.googleTrendsGeo = null;

        NovelReaderSettings settings = new NovelReaderSettings();
        settings.loadState(state);

        assertEquals("baidu", settings.getHotSearchSource());
        assertEquals("", settings.getXTrendsRegion());
        assertEquals("US", settings.getGoogleTrendsGeo());
    }
}
