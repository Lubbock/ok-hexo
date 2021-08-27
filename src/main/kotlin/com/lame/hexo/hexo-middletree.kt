package com.lame.hexo

import HexoHeader
import com.google.gson.Gson

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

data class TreeNode(val name: String, var children: List<TreeNode>?, val value: Int?)

fun generateMiddleTree(headers: ArrayList<HexoHeader>): String {
    val gson=Gson()
    val root = TreeNode("随记", null, null)
    val tags = HashSet<String>()
    val categories = HashSet<String>()
    val tagsTf = HashMap<String, Int>()

    val cgSitMap = HashMap<String, LinkedList<HexoHeader>>()
    headers.forEach {
        tags.addAll(it.tags)
        it.tags.forEach { tag ->
            var count = tagsTf[tag] ?: 1
            tagsTf[tag] = ++count
        }
        categories.addAll(it.categories)
        it.categories.forEach { cg->
            val docs = cgSitMap[cg] ?: LinkedList()
            docs.add(it)
            cgSitMap[cg] = docs
        }
    }

    val cgNodes = LinkedList<TreeNode>()
    cgSitMap.forEach { t, u ->
        val fileNodes = LinkedList<TreeNode>()
        val secondCg = TreeNode(t, fileNodes, null)
        u.forEach { doc->
            val tagNodes = LinkedList<TreeNode>()
            val fileNode = TreeNode(doc.title.substringBeforeLast("."), tagNodes, null)
            fileNodes.add(fileNode)
            doc.tags.forEach { tagNodes.add(TreeNode(it,null,tagsTf[it])) }
        }
        cgNodes.add(secondCg)
    }
    root.children = cgNodes
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //root -> cgNode->fileNode->tagNode
    var tpl = """
---
title: 随笔记概要
date: ${sdf.format(Date())}
---
随笔记概要
{% echarts 800 '100%' %}

    option = {
        tooltip: {
            trigger: 'item',
            triggerOn: 'mousemove'
        },
        series: [
            {
                type: 'tree',

                data: [${gson.toJson(root)}],

                top: '18%',
                bottom: '14%',

                layout: 'radial',

                symbol: 'emptyCircle',

                symbolSize: 7,

                initialTreeDepth: 3,

                animationDurationUpdate: 750,

                emphasis: {
                    focus: 'descendant'
                }
            }
        ]
    }
{% endecharts %}
""".trimIndent()
    return tpl
}

