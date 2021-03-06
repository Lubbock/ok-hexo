package com.lame.hexo

import com.google.gson.Gson
import listModifyFile
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FalseFileFilter
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import java.io.InputStreamReader

import java.io.BufferedReader
import java.lang.StringBuilder
import kotlin.collections.HashMap


data class Hexo(val baseDir: String, val img: String, val post: String)

data class HexoHeader(val title: String, val date: Date, val tags: List<String>, val categories: List<String>)

fun productHexoDescHeader(header: HexoHeader): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val normalize = fun(x_normalize: List<String>): String {
        val nm = StringBuilder("")
        for (x in x_normalize) {
            nm.append(" - $x")
            nm.append("\n")
        }
        return nm.toString()
    }
    val tags = if (header.tags.isEmpty()){ "" }else{"tags:\n${normalize(header.tags)}"}
    val categories = if (header.categories.isEmpty()){ "" }else{"categories:\n${normalize(header.categories)}"}
    return "---\ntitle: ${header.title}\ndate: ${sdf.format(Date())}\n$tags$categories\n---"

}

fun copyDir(src: String, target: String) {
    FileUtils.copyDirectory(File(src), File(target))
}
//const val regex = "!\\[.*\\]\\(.*\\)"
const val regex = "!\\[.*]\\(.*\\)"
val pattern: Pattern = Pattern.compile(regex)
fun innerImageTransform(line: String): String {
    if (line.contains("![") && line.contains("assets")) {
        val imgLink = ArrayList<String>()
        line.split("![").forEach { s ->
            val m = pattern.matcher("![$s")
            if (m.find()) {
                imgLink.add("![$s".substring(m.start(), m.end()))
            }
        }
        var transformLine = line
        for (link in imgLink) {
            val transformLink = "${link.substringBefore("(")}(/images/${link.substringAfter("(")}"
            //??????????????????????????????
            if (transformLink.contains("http://") || transformLine.contains("https://")) {
                continue
            }
            transformLine = transformLine.replace(link, transformLink)
        }
        return transformLine
    } else {
        return line
    }
}

/**
 * typora ??????????????????hexo??????
 * @param fp - ????????????
 * @param hexo - Hexo??????
 * */
private fun hexoImgTrans(fp: String, hexo: Hexo) {
    val imgs = FileUtils.listFilesAndDirs(File(fp), FalseFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
    imgs.filter { it.name.endsWith("assets") }
        .forEach {
            println("??????????????????${it.absolutePath}")
            copyDir(it.absolutePath, File(hexo.img, it.name).absolutePath)
        }
}



data class OkHexoDoc(val name: String = "", val contents: ArrayList<String> = ArrayList(200))

data class Tag(val name: String, val tags: String?)

/**
 * ??????md -> doc -> ?????????tags????????? (python) -> ??????
 * */
private fun stragey02(f: File, fp: String, hexo: Hexo):ArrayList<HexoHeader> {
    val lines = FileUtils.readLines(f)
    var cgs = f.parentFile.absolutePath.substring(File(fp).absolutePath.length).split("/").filter { it.isNotEmpty() }
    if (cgs.isEmpty()) {
        cgs = arrayListOf("root")
    }
    val docs = ArrayList<OkHexoDoc>(20)
    var okdoc = OkHexoDoc(f.name)
    docs.add(okdoc)
    var isRoot = true
    var codeBut = false
    //?????????????????????????????????
    lines.map { innerImageTransform(it) }
        .forEach {
            if (it.startsWith("```")) {
                codeBut = !codeBut
            }
            if (it.startsWith("# ") && !isRoot && !codeBut) {
                okdoc = OkHexoDoc("${f.name.substringBeforeLast(".")}_${it.substringAfterLast("# ")}.md")
                okdoc.contents.add(it)
                docs.add(okdoc)
            } else {
                okdoc.contents.add(it)
            }
            isRoot = false
        }

    val tempDirectoryPath = FileUtils.getTempDirectoryPath() + "/" + System.currentTimeMillis()
    val docFm = HashMap<String, OkHexoDoc>(50)
    docs.forEach { doc ->
        var isCodeBut = false
        val temp = ArrayList<String>()
        doc.contents.forEach {
            if (it.startsWith("```")) {
                isCodeBut = !isCodeBut
            } else {
                if (!isCodeBut) {
                    temp.add(it)
                }
            }
        }
        FileUtils.writeLines(File(tempDirectoryPath, doc.name), temp)
        docFm[doc.name] = doc
    }
    val proc = Runtime.getRuntime()
        .exec("/usr/bin/python3 /media/lame/0DD80F300DD80F30/code/ok-hexo/scripts/py-text/extract.py $tempDirectoryPath")
    val reader = BufferedReader(InputStreamReader(proc.inputStream))
    var line: String?
    val predicateTags = HashMap<String, Tag>(100)
    while (reader.readLine().also { line = it } != null) {
        if (line != null) {
//            *********Haskell.md
//            ghci,??????,haskell,sh,mod
//            *********????????????.md
//            ??????,??????,??????,??????,??????
            if (line?.startsWith("*********") == true) {
                val fname = line?.substringAfterLast("*********")
                if (fname != null) {
                    val tagline = reader.readLine()
                    predicateTags[fname] = Tag(fname, tagline)
                }
            }
        }
    }

    val headers = ArrayList<HexoHeader>()
    docs.forEach { it ->
        f.name.substringBeforeLast(".")
        val ptag = predicateTags.getOrDefault(it.name, Tag(it.name, ""))
        val ntags = ArrayList<String>()
        ptag.tags?.split(",")?.forEach {
            if (it.trim().isNotEmpty()) {
                ntags.add(it)
            }
        }
        val header = HexoHeader(it.name, Date(f.lastModified()), ntags, cgs)
        headers.add(header)
        println(productHexoDescHeader(header))
        it.contents.add(0, productHexoDescHeader(header))
        FileUtils.writeLines(File(hexo.post, it.name), "utf-8", it.contents)
    }
    return headers
}

data class HexoHeaders(val headers: List<HexoHeader>)

private fun generateHeaders(fp: String, headers: HexoHeaders): List<HexoHeader> {
    val modifyCache = ".okheader"
    val gson = Gson()
    val rtfile = File(fp, modifyCache)
    if (rtfile.exists()) {
        val cacheHeaders =
            gson.fromJson(FileUtils.readFileToString(File(fp, modifyCache)), HexoHeaders::class.java)?.headers
        val cacheHh = HashMap<String, HexoHeader>()
        cacheHeaders?.forEach { cacheHh[it.title] = it }
        headers.headers.forEach { cacheHh[it.title] = it }
        FileUtils.writeLines(File(fp, modifyCache), arrayListOf(gson.toJson(HexoHeaders(cacheHh.values.toList()))))
        return cacheHh.values.toList()
    } else {
        FileUtils.writeLines(File(fp, modifyCache), arrayListOf(gson.toJson(headers)))
        return headers.headers
    }
}
/**
 * typora hexo ????????????
 * @param fp - ????????????
 * @param hexo - hexo??????
 *
 * */
private fun hexoMdTrans(fp: String, hexo: Hexo) {
    val mds = FileUtils.listFiles(File(fp), FileFilterUtils.suffixFileFilter("md"), TrueFileFilter.INSTANCE)
    val changeFile = listModifyFile(fp, mds)
    var headers = ArrayList<HexoHeader>()
    val modifyFiles = ArrayList<File>()
    println(changeFile)
    changeFile.add?.apply { modifyFiles.addAll(this) }
    changeFile.update?.apply { modifyFiles.addAll(this) }
    modifyFiles.forEach {
        headers.addAll(stragey02(it, fp, hexo))
    }
    headers = ArrayList(generateHeaders(fp, HexoHeaders(headers)))
    val random=Random()
    val begin = random.nextInt(10)
    val subHeaders = if (headers.size<20){
        headers
    }else{
        headers.subList(begin, begin + 20)
    }
    val aboutPage = generateMiddleTree(ArrayList(subHeaders))
    FileUtils.writeLines(File(hexo.baseDir,"source/about/index.md"),
        "utf-8", arrayListOf(aboutPage))
}


fun hexoTrans(fp: String, hexo: Hexo) {
    hexoImgTrans(fp, hexo)
    hexoMdTrans(fp, hexo)
}

//?????? ??????????????????????????????????????????????????????,??????????????????

fun main() {
    val hexo =
        Hexo(
            "/media/lame/0DD80F300DD80F30/bread17/blog",
            "/media/lame/0DD80F300DD80F30/bread17/blog/source/images",
            "/media/lame/0DD80F300DD80F30/bread17/blog/source/_posts"
        )
    hexoTrans("/media/lame/0DD80F300DD80F30/document", hexo)
    println("??????????????????")
}