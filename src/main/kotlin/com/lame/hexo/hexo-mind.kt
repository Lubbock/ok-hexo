package com.lame.hexo

import com.google.gson.Gson

import java.text.SimpleDateFormat
import java.util.*

import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

data class EchartCategory(val data: Array<String>)
data class EchartAliex(val name: String)

data class EchartNode(
    val id: String,
    val name: String,
    var symbolSize: Double,
    var x: Double,
    var y: Double,
    var value: Double,
    val category: Int
)

data class EchartNodeLink(val source: String, val target: String)

data class TagScore(val nodeId: String, val score: Double, val linkCount: Int)

fun generateMindOptions(headers: ArrayList<HexoHeader>): String {
    //[{"data":["c1","c2","c3"]}],
    val gson = Gson()
    val tags = HashSet<String>()
    val categories = HashSet<String>()
    val aliexs = ArrayList<EchartAliex>()
    val tagsTf = HashMap<String, Int>()

    headers.forEach {
        tags.addAll(it.tags)
        it.tags.forEach { tag ->
            var count = tagsTf[tag] ?: 1
            tagsTf[tag] = ++count
        }
        categories.add(it.categories[0])
    }


    val echartCategory = EchartCategory(categories.toArray(arrayOf()))
    val cgMap = HashMap<String, Int>()
    echartCategory.data.forEachIndexed { index, it ->
        aliexs.add(EchartAliex(it))
        cgMap.put(it, index)
    }

    //包含tag 并且 value得分最高 多个tag，做多条连线
    val tagMaxScore = HashMap<String, TagScore>()
    val siteNode = HashMap<String, EchartNode>()
    val nodes = headers.mapIndexed { index, it ->
        val value = it.tags.map { e -> tagsTf[e] ?: 0 }.sum() + 0.0
        it.tags.forEach { tag ->
            val score = tagMaxScore[tag]?.score ?: 0.0
            val linkCount = tagMaxScore[tag]?.linkCount ?: 0 + 1
            if (value > score) {
                tagMaxScore[tag] = TagScore("$index", value, linkCount)
            }
        }
        val node = EchartNode(
            "$index",
            it.title,
            value,
            (Math.random() * (300 + 300) - 300),
            (Math.random() * (300 + 300) - 300),
            value,
            cgMap[it.categories[0]] ?: 0
        )
        siteNode["$index"] = node
        node
    }.toList()
    tagMaxScore.values.map { it.linkCount }.sum()
    var i = 0
    tagMaxScore.forEach { (t, u) ->
        val changeNode = siteNode[t]
        if (changeNode != null) {
            changeNode.value = 20 + u.linkCount * 4.5
            changeNode.symbolSize = 20 + u.linkCount * 5.0
            changeNode.x = 600.0 / tags.size + 10 * (u.linkCount + i++) - 300
            changeNode.y = 600.0 / tags.size + 10 * (u.linkCount + i) - 300
        }
    }

    val links = ArrayList<EchartNodeLink>()
    headers.forEachIndexed { index, it ->
        it.tags.forEach { tag ->
            val maxScore = tagMaxScore[tag]
            if (maxScore != null) {
                if ((maxScore.nodeId.toInt()) != index) {
                    val node = siteNode["$index"]
                    val linkNode = siteNode[maxScore.nodeId]
                    if (node != null && linkNode != null) {
                        when (linkNode.x) {
                            in 150.0..30000.0 -> node.x = linkNode.x + (Math.random() * (300 - 160) + 160)
                            in 0.0..150.0 -> node.x = linkNode.x - (Math.random() * (140 - 0) + 0)
                            in -150.0..0.0 -> node.x = linkNode.x - (Math.random() * (0 + 150) - 150)
                            in -30000.0..-150.0 -> node.x = linkNode.x - (Math.random() * (-150 + 300) - 300)
                        }
                        when (linkNode.y) {
                            in 150.0..30000.0 -> node.y = linkNode.y + (Math.random() * (310 - 160) + 160)
                            in 0.0..150.0 -> node.y = linkNode.y - (Math.random() * (140 - 0) + 0)
                            in -150.0..0.0 -> node.y = linkNode.y - (Math.random() * (0 + 150) - 150)
                            in -30000.0..-150.0 -> node.y = linkNode.y - (Math.random() * (-150 + 300) - 300)
                        }
                        links.add(EchartNodeLink("$index", maxScore.nodeId))
                    }
                }
            }
        }
    }
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //value 文档 tags得分的综合
    // link link根据tags相关程度，连接文档取最相关的
    //category 分类只取一级
    return """
---
title: 随笔记概要
date: ${sdf.format(Date())}
---
随笔记概要
{% echarts 800 '100%' %}

    option = {
        tooltip: {},
        legend: [${gson.toJson(echartCategory)}],
         animationDuration: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [
            {
                name: '随笔记',
                type: 'graph',
                layout: 'none',
                data:  ${gson.toJson(nodes)},
                links: ${gson.toJson(links)},
                categories: ${gson.toJson(aliexs)},
                roam: true,
                label: {
                    show: true,
                    position: 'right',
                    formatter: '{b}'
                },
                lineStyle: {
                    color: 'source',
                    curveness: 0.3
                },
                 emphasis: {
                    focus: 'adjacency',
                    lineStyle: {
                        width: 10
                    }
                }
            }
        ]
    };
{% endecharts %}
""".trimIndent()
}
