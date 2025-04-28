package com.brandon3055.brandonscore.lib;

import net.minecraft.resources.ResourceLocation;

/**
 * Created by brandon3055 on 13/09/2016.
 *
 */
public class DLResource {

    public final ResourceLocation resource;
    public volatile int width = 0;
    public volatile int height = 0;
    public volatile boolean sizeSet = false;
    public volatile boolean dlFailed = false;
    public volatile boolean dlFinished = false;
    public boolean lastCheckStatus = false;

    public DLResource(String resourceDomainIn, String url) {
        this.resource = ResourceLocation.fromNamespaceAndPath(resourceDomainIn, url);
    }

    /**
     * @return true once when the download completes
     */
    public boolean dlStateChanged() {
        if (dlFinished && !lastCheckStatus) {
            lastCheckStatus = true;
            return true;
        }
        return false;
    }
}
