package com.fish.toucher.ui;

import com.google.gson.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 热搜响应解析器。所有外部字段在进入 UI 前统一限制长度并校验 URL。
 *
 * @author fengshi
 */
final class HotSearchParser {

    private static final int MAX_ITEMS = 50;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_TAG_LENGTH = 50;
    private static final int MAX_URL_LENGTH = 2048;
    private static final Pattern X_ITEM_PATTERN = Pattern.compile(
            "trend-name[^<]*<a\\s+href=\"([^\"]+)\"[^>]*>([^<]+)</a>"
    );

    private HotSearchParser() {}

    static List<HotSearchManager.HotSearchItem> parse(String source, String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            return switch (source) {
                case "toutiao" -> parseToutiao(body);
                case "zhihu" -> parseZhihu(body);
                case "douyin" -> parseDouyin(body);
                case "kuaishou" -> parseKuaishou(body);
                case "x" -> parseX(body);
                case "google" -> parseGoogle(body);
                default -> parseBaidu(body);
            };
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<HotSearchManager.HotSearchItem> parseBaidu(String body) {
        List<JsonObject> objects = new ArrayList<>();
        collectObjects(JsonParser.parseString(body), objects, "word");
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        int fallbackRank = 0;
        for (JsonObject object : objects) {
            String title = text(object, "word");
            if (title.isEmpty()) continue;
            int rank = integer(object, "index", fallbackRank++);
            add(result, rank, title, text(object, "hotTag"), text(object, "url"));
            if (result.size() >= MAX_ITEMS) break;
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseToutiao(String body) {
        JsonArray data = array(object(JsonParser.parseString(body)), "data");
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject item = object(element);
            add(
                    result,
                    result.size(),
                    text(item, "Title"),
                    text(item, "HotValue"),
                    text(item, "Url")
            );
            if (result.size() >= MAX_ITEMS) break;
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseZhihu(String body) {
        JsonArray data = array(object(JsonParser.parseString(body)), "data");
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject target = object(object(element).get("target"));
            long id = longValue(target, "id", 0);
            String url = id > 0 ? "https://www.zhihu.com/question/" + id : "";
            add(result, result.size(), text(target, "title"), "", url);
            if (result.size() >= MAX_ITEMS) break;
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseDouyin(String body) {
        JsonArray data = array(object(JsonParser.parseString(body)), "word_list");
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject item = object(element);
            String title = text(item, "word");
            String url = "https://www.douyin.com/search/"
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
            add(result, result.size(), title, text(item, "hot_value"), url);
            if (result.size() >= MAX_ITEMS) break;
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseKuaishou(String body) {
        JsonObject root = object(JsonParser.parseString(body));
        JsonObject data = object(root.get("data"));
        JsonObject rank = object(data.get("visionHotRank"));
        JsonArray items = array(rank, "items");
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        for (JsonElement element : items) {
            JsonObject item = object(element);
            String title = text(item, "name");
            String url = "https://www.kuaishou.com/search/video?searchKey="
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
            add(
                    result,
                    integer(item, "rank", result.size()),
                    title,
                    text(item, "hotValue"),
                    url
            );
            if (result.size() >= MAX_ITEMS) break;
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseX(String body) {
        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        Matcher matcher = X_ITEM_PATTERN.matcher(body);
        URI base = URI.create("https://trends24.in/");
        while (matcher.find() && result.size() < MAX_ITEMS) {
            String url = base.resolve(matcher.group(1)).toString();
            add(result, result.size(), decodeHtml(matcher.group(2)), "", url);
        }
        return result;
    }

    private static List<HotSearchManager.HotSearchItem> parseGoogle(String body) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        List<HotSearchManager.HotSearchItem> result = new ArrayList<>();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(body));
        boolean inItem = false;
        String title = "";
        String traffic = "";
        while (reader.hasNext() && result.size() < MAX_ITEMS) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("item".equals(name)) {
                    inItem = true;
                    title = "";
                    traffic = "";
                } else if (inItem && "title".equals(name)) {
                    title = reader.getElementText();
                } else if (inItem && "approx_traffic".equals(name)) {
                    traffic = reader.getElementText();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && inItem
                    && "item".equals(reader.getLocalName())) {
                String url = "https://www.google.com/search?q="
                        + URLEncoder.encode(title, StandardCharsets.UTF_8);
                add(result, result.size(), title, traffic, url);
                inItem = false;
            }
        }
        reader.close();
        return result;
    }

    private static void add(
            List<HotSearchManager.HotSearchItem> result,
            int rank,
            String title,
            String tag,
            String url
    ) {
        String safeTitle = limit(title == null ? "" : title.trim(), MAX_TITLE_LENGTH);
        if (safeTitle.isEmpty() || result.size() >= MAX_ITEMS) {
            return;
        }
        result.add(new HotSearchManager.HotSearchItem(
                Math.max(0, rank),
                safeTitle,
                limit(tag == null ? "" : tag.trim(), MAX_TAG_LENGTH),
                safeHttpUrl(url)
        ));
    }

    static String safeHttpUrl(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_URL_LENGTH) {
            return "";
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {
                return "";
            }
            return uri.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private static String decodeHtml(String value) {
        return value.replace("&amp;", "&")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static void collectObjects(JsonElement element, List<JsonObject> result, String key) {
        if (element == null || element.isJsonNull() || result.size() >= MAX_ITEMS * 4) return;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectObjects(child, result, key);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has(key)) result.add(object);
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collectObjects(entry.getValue(), result, key);
            }
        }
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject()
                ? element.getAsJsonObject() : new JsonObject();
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray()
                ? element.getAsJsonArray() : new JsonArray();
    }

    private static String text(JsonObject object, String key) {
        try {
            JsonElement element = object.get(key);
            return element == null || element.isJsonNull() ? "" : element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        try {
            return object.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
