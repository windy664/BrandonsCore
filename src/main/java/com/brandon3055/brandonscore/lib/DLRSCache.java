package com.brandon3055.brandonscore.lib;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.handlers.FileHandler;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by brandon3055 on 13/09/2016.
 * Downloadable Resource Cache
 */
@Deprecated //TODO re write this.
public class DLRSCache {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static final DLResource DOWNLOADING_TEXTURE = new DLResource(BrandonsCore.MODID, "textures/loading_texture.png");
    static {
        DOWNLOADING_TEXTURE.height = 16;
        DOWNLOADING_TEXTURE.width = 16;
        DOWNLOADING_TEXTURE.sizeSet = true;
    }
    private static Map<String, DLResource> resourceCache = new HashMap<>();
    private static File cacheFolder;
    private static Pattern urlStripper = Pattern.compile("([^a-zA-Z0-9]*)");

    public static void init() {
        LOCK.lock();

        cacheFolder = new File(FileHandler.brandon3055Folder, "ResourceCache");
        cacheFolder.mkdirs();
    }


    public static DLResource getResource(String url) {
        String key = urlStripper.matcher(url).replaceAll("_").toLowerCase(Locale.ENGLISH);

        if (!resourceCache.containsKey(key)) {
            DLResource resourceLocation = new DLResource(BrandonsCore.MODID, key);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();

            File cache = new File(cacheFolder, "Cache#" + url.hashCode() + ".png");

            ThreadedImageDownloader downloader = new ThreadedImageDownloader(cache, url, DOWNLOADING_TEXTURE);
            downloader.setDlLocation(resourceLocation);
            texturemanager.register(resourceLocation.resource, downloader);

            resourceCache.put(key, resourceLocation);
        }

        return resourceCache.get(key);
    }

    public static void clearResourceCache() {
        resourceCache.clear();
    }

    public static void clearFileCache() {
        clearResourceCache();
        File[] files = cacheFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("Cache#")) {
                    file.delete();
                }
            }
        }
    }

    public static void clearResourceCache(String url) {
        String key = urlStripper.matcher(url).replaceAll("_").toLowerCase(Locale.ENGLISH);
        resourceCache.remove(key);
    }

    public static void clearFileCache(String url) {
        clearResourceCache(url);
        File[] files = cacheFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("Cache#" + url.hashCode() + ".png")) {
                    file.delete();
                    return;
                }
            }
        }
    }
}
