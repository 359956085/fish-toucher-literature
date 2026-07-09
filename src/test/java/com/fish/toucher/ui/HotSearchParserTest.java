package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HotSearchParserTest {

    @Test
    void 应解析JSON来源并编码搜索词() {
        List<HotSearchManager.HotSearchItem> douyin = HotSearchParser.parse(
                "douyin",
                "{\"word_list\":[{\"word\":\"空 格/测试\",\"hot_value\":123}]}"
        );
        assertEquals(1, douyin.size());
        assertEquals("空 格/测试", douyin.get(0).word());
        assertTrue(douyin.get(0).url().contains("%E7%A9%BA+%E6%A0%BC%2F"));

        List<HotSearchManager.HotSearchItem> zhihu = HotSearchParser.parse(
                "zhihu",
                "{\"data\":[{\"target\":{\"id\":42,\"title\":\"问题\"}}]}"
        );
        assertEquals("https://www.zhihu.com/question/42", zhihu.get(0).url());
    }

    @Test
    void 应限制条目数量和字段长度() {
        StringBuilder json = new StringBuilder("{\"word_list\":[");
        for (int index = 0; index < 80; index++) {
            if (index > 0) json.append(',');
            json.append("{\"word\":\"")
                    .append("x".repeat(250))
                    .append(index)
                    .append("\",\"hot_value\":1}");
        }
        json.append("]}");

        List<HotSearchManager.HotSearchItem> items =
                HotSearchParser.parse("douyin", json.toString());
        assertEquals(50, items.size());
        assertEquals(200, items.get(0).word().length());
    }

    @Test
    void 应拒绝非HTTP链接和损坏响应() {
        assertEquals("", HotSearchParser.safeHttpUrl("file:///c:/secret.txt"));
        assertEquals("", HotSearchParser.safeHttpUrl("javascript:alert(1)"));
        assertEquals(
                "https://example.com/a",
                HotSearchParser.safeHttpUrl("https://example.com/a")
        );
        assertTrue(HotSearchParser.parse("baidu", "{bad").isEmpty());
    }

    @Test
    void 应安全解析GoogleRss() {
        String xml = """
                <rss xmlns:ht="https://trends.google.com/trending/rss">
                  <channel>
                    <item>
                      <title>A &amp; B</title>
                      <ht:approx_traffic>100+</ht:approx_traffic>
                    </item>
                  </channel>
                </rss>
                """;
        List<HotSearchManager.HotSearchItem> items =
                HotSearchParser.parse("google", xml);
        assertEquals(1, items.size());
        assertEquals("A & B", items.get(0).word());
        assertEquals("100+", items.get(0).hotTag());
    }
}
