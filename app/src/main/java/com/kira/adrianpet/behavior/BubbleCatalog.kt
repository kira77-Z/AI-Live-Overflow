package com.kira.adrianpet.behavior

class BubbleCatalog(
    private val chooseIndex: (size: Int) -> Int = { size -> (0 until size).random() },
    private val hourProvider: () -> Int = { java.time.LocalTime.now().hour },
) {
    private val taps = listOf("在呢。", "戳到啦。", "嗯？")
    private val mornings = listOf("早呀，小猫。", "醒来就看见你了。", "今天也陪着你。")
    private val nights = listOf("这么晚还不睡？", "夜里也在呢。", "再陪你一会儿。")
    private val rapidTaps = listOf("好痒……", "慢一点。", "被发现了。")
    private val drags = listOf("要带我去哪？", "抓稳了。", "我会自己走。")

    fun tap(): String = when (hourProvider()) {
        in 5..10 -> choose(mornings)
        23, in 0..4 -> choose(nights)
        else -> choose(taps)
    }
    fun rapidTap(): String = choose(rapidTaps)
    fun drag(): String = choose(drags)
    fun charging(): String = "吃到电啦。"
    fun unplugged(): String = "电量先省着用。"
    fun sleep(): String = "陪你安静一会儿。"

    private fun choose(items: List<String>): String = items[Math.floorMod(chooseIndex(items.size), items.size)]
}
