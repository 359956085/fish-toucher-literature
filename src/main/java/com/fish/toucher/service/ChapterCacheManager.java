package com.fish.toucher.service;

import com.fish.toucher.model.ChapterInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChapterCacheManager {

    private static final Logger LOG = Logger.getInstance(ChapterCacheManager.class);
    private static final ChapterCacheManager INSTANCE = new ChapterCacheManager();

    private static final long CHAPTER_LIST_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final Type CHAPTER_LIST_TYPE = new TypeToken<List<ChapterInfo>>() {}.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path cacheDir;

    private ChapterCacheManager() {
        cacheDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher", "cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            LOG.error("Failed to create cache directory: " + cacheDir, e);
        }
    }

    public static ChapterCacheManager getInstance() {
        return INSTANCE;
    }

    public List<ChapterInfo> getCachedChapterList(String bookUrl) {
        Path bookDir = getBookCacheDir(bookUrl);
        Path file = bookDir.resolve("chapters.json");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            long lastModified = Files.getLastModifiedTime(file).toMillis();
            if (System.currentTimeMillis() - lastModified > CHAPTER_LIST_TTL_MS) {
                LOG.info("Chapter list cache expired for: " + bookUrl);
                return null;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return gson.fromJson(json, CHAPTER_LIST_TYPE);
        } catch (Exception e) {
            LOG.warn("Failed to read cached chapter list", e);
            return null;
        }
    }

    public void cacheChapterList(String bookUrl, List<ChapterInfo> chapters) {
        Path bookDir = getBookCacheDir(bookUrl);
        try {
            Files.createDirectories(bookDir);
            String json = gson.toJson(chapters, CHAPTER_LIST_TYPE);
            Files.writeString(bookDir.resolve("chapters.json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to cache chapter list", e);
        }
    }

    public String getCachedContent(String bookUrl, int chapterIndex) {
        Path bookDir = getBookCacheDir(bookUrl);
        Path file = bookDir.resolve(chapterIndex + ".txt");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read cached content for chapter " + chapterIndex, e);
            return null;
        }
    }

    public void cacheContent(String bookUrl, int chapterIndex, String content) {
        Path bookDir = getBookCacheDir(bookUrl);
        try {
            Files.createDirectories(bookDir);
            Files.writeString(bookDir.resolve(chapterIndex + ".txt"), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to cache content for chapter " + chapterIndex, e);
        }
    }

    public void clearCache(String bookUrl) {
        Path bookDir = getBookCacheDir(bookUrl);
        if (!Files.isDirectory(bookDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir)) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(bookDir);
            LOG.info("Cleared cache for: " + bookUrl);
        } catch (IOException e) {
            LOG.error("Failed to clear cache for: " + bookUrl, e);
        }
    }

    private Path getBookCacheDir(String bookUrl) {
        return cacheDir.resolve(md5(bookUrl));
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available in standard JVMs
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}
