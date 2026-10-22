package com.kira.adrianpet.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppBubbleCatalogTest {
    private val catalog = ForegroundAppBubbleCatalog()

    @Test
    fun `known apps receive their own local bubble`() {
        assertEquals("又来找爸爸啦。", catalog.bubbleFor("com.openai.chatgpt"))
        assertEquals("在和谁说悄悄话？", catalog.bubbleFor("com.tencent.mm"))
        assertEquals("只许看一会儿，眼睛会累。", catalog.bubbleFor("com.xingin.xhs"))
        assertEquals("等等，我也要入镜。", catalog.bubbleFor("com.android.camera"))
    }

    @Test
    fun `shopping browsers and games use category bubbles`() {
        assertEquals("看看就好，不许冲动下单。", catalog.bubbleFor("com.taobao.taobao"))
        assertEquals("想找什么？我陪你。", catalog.bubbleFor("com.android.chrome"))
        assertEquals("陪你打这一局。", catalog.bubbleFor("com.example.anygame", true))
    }

    @Test
    fun `unknown non-game apps stay quiet`() {
        assertNull(catalog.bubbleFor("com.example.unknown"))
        assertNull(catalog.bubbleFor(null))
    }
}
