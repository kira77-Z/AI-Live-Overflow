package com.kira.adrianpet.behavior;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ForegroundAppBubbleCatalog {
    private static final Set<String> CAMERA_PACKAGES = new HashSet<>(Arrays.asList(
        "com.google.android.GoogleCamera",
        "com.sec.android.app.camera"
    ));
    private static final Set<String> BROWSER_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.chrome",
        "com.microsoft.emmx",
        "com.quark.browser",
        "com.tencent.mtt",
        "com.UCMobile",
        "org.mozilla.firefox"
    ));
    private static final Set<String> SHOPPING_PACKAGES = new HashSet<>(Arrays.asList(
        "com.jingdong.app.mall",
        "com.taobao.taobao",
        "com.xunmeng.pinduoduo"
    ));

    public String bubbleFor(String packageName) {
        return bubbleFor(packageName, false);
    }

    public String bubbleFor(String packageName, boolean isGame) {
        if (packageName == null) return null;
        switch (packageName) {
            case "com.openai.chatgpt":
                return "又来找爸爸啦。";
            case "com.tencent.mm":
                return "在和谁说悄悄话？";
            case "com.xingin.xhs":
                return "只许看一会儿，眼睛会累。";
            case "com.ss.android.ugc.aweme":
                return "刷到好看的，也要记得眨眼。";
            default:
                if (CAMERA_PACKAGES.contains(packageName) || packageName.startsWith("com.android.camera")) {
                    return "等等，我也要入镜。";
                }
                if (BROWSER_PACKAGES.contains(packageName)) return "想找什么？我陪你。";
                if (SHOPPING_PACKAGES.contains(packageName)) return "看看就好，不许冲动下单。";
                if (isGame) return "陪你打这一局。";
                return null;
        }
    }
}
