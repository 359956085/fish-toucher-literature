package com.fish.toucher.service;

import com.fish.toucher.model.BookSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookSourceManager {

    private static final Logger LOG = Logger.getInstance(BookSourceManager.class);
    private static final BookSourceManager INSTANCE = new BookSourceManager();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path sourcesDir;
    private final List<BookSource> sources = new ArrayList<>();

    private BookSourceManager() {
        sourcesDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher", "sources");
        try {
            Files.createDirectories(sourcesDir);
        } catch (IOException e) {
            LOG.error("Failed to create sources directory: " + sourcesDir, e);
        }
        loadAll();
    }

    public static BookSourceManager getInstance() {
        return INSTANCE;
    }

    public void loadAll() {
        sources.clear();
        if (!Files.isDirectory(sourcesDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcesDir, "*.json")) {
            for (Path file : stream) {
                try {
                    String json = Files.readString(file, StandardCharsets.UTF_8);
                    BookSource source = gson.fromJson(json, BookSource.class);
                    if (source != null && source.getName() != null) {
                        sources.add(source);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to parse book source file: " + file, e);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to scan sources directory", e);
        }
        LOG.info("Loaded " + sources.size() + " book sources");
    }

    public List<BookSource> getSources() {
        return new ArrayList<>(sources);
    }

    public List<BookSource> getEnabledSources() {
        return sources.stream()
                .filter(BookSource::isEnabled)
                .collect(Collectors.toList());
    }

    public void save(BookSource source) {
        String filename = sanitizeName(source.getName()) + ".json";
        Path file = sourcesDir.resolve(filename);
        try {
            String json = gson.toJson(source);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            // Update in-memory list
            sources.removeIf(s -> s.getName().equals(source.getName()));
            sources.add(source);
            LOG.info("Saved book source: " + source.getName());
        } catch (IOException e) {
            LOG.error("Failed to save book source: " + source.getName(), e);
        }
    }

    public void delete(BookSource source) {
        String filename = sanitizeName(source.getName()) + ".json";
        Path file = sourcesDir.resolve(filename);
        try {
            Files.deleteIfExists(file);
            sources.removeIf(s -> s.getName().equals(source.getName()));
            LOG.info("Deleted book source: " + source.getName());
        } catch (IOException e) {
            LOG.error("Failed to delete book source: " + source.getName(), e);
        }
    }

    public void importFromJson(String json) {
        JsonElement element = JsonParser.parseString(json);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                importSingleSource(item.toString());
            }
        } else if (element.isJsonObject()) {
            importSingleSource(json);
        } else {
            LOG.warn("Invalid JSON for import: not an object or array");
        }
    }

    private void importSingleSource(String json) {
        BookSource source = gson.fromJson(json, BookSource.class);
        if (source == null || source.getName() == null || source.getName().isEmpty()) {
            LOG.warn("Import skipped: source has no name");
            return;
        }
        if (source.getSearchRule() == null) {
            LOG.warn("Import skipped: source '" + source.getName() + "' has no search rule");
            return;
        }
        if (source.getChapterRule() == null) {
            LOG.warn("Import skipped: source '" + source.getName() + "' has no chapter rule");
            return;
        }
        if (source.getContentRule() == null) {
            LOG.warn("Import skipped: source '" + source.getName() + "' has no content rule");
            return;
        }
        save(source);
    }

    public String exportToJson(BookSource source) {
        return gson.toJson(source);
    }

    public String exportAllToJson() {
        return gson.toJson(sources);
    }

    private String sanitizeName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_\\-]", "_");
    }
}
