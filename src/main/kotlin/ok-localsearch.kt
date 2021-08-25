class BmMatch(pattern: String) {
    private val patternBytes = pattern.toByteArray()
    private val patternSize = patternBytes.size
    private val badSkip = MutableList((Byte.MAX_VALUE - Byte.MIN_VALUE + 1)) { pattern.toByteArray().size }
    private val goodSkip = MutableList(pattern.toByteArray().size) { patternSize }

    init {
        // 构建坏字符跳转记录表
        var pLen = patternSize
        for (b in patternBytes) {
            badSkip[b - Byte.MIN_VALUE] = --pLen
        }

        // 构建好后缀跳转记录表
        val suff = MutableList(patternSize) { patternSize }
        for (i in (patternSize - 2 downTo 0)) {
            var q = i
            while (q >= 0 && patternBytes[q] == patternBytes[patternSize - 1 - i + q]) {
                --q
            }
            suff[i] = i - q
        }
        for (i in (patternSize - 1 downTo 0)) {
            if (suff[i] == i + 1) {
                for (j in (0 until patternSize - 1 - i)) {
                    if (goodSkip[j] == patternSize) {
                        goodSkip[j] = patternSize - 1 - i
                    }
                }
            }
        }
        for (i in (0 until patternSize - 1)) {
            goodSkip[patternSize - 1 - suff[i]] = patternSize - 1 - i
        }
    }

    fun match(txt: String): List<Int> {
        val txtBytes = txt.toByteArray()
        val txtBytesSize = txtBytes.size
        var txtIndex = 0
        val res = mutableListOf<Int>()
        while (txtIndex <= txtBytesSize - patternSize) {
            var ptnIndex = patternSize - 1
            while (ptnIndex >= 0 && patternBytes[ptnIndex] == txtBytes[ptnIndex + txtIndex]) {
                --ptnIndex
            }
            txtIndex += if (ptnIndex < 0) {
                res.add(txtIndex)
                goodSkip[0]
            } else {
                maxOf(
                    goodSkip[ptnIndex],
                    badSkip[txtBytes[ptnIndex + txtIndex] - Byte.MIN_VALUE] - patternSize + 1 + ptnIndex
                )
            }
        }
        return res
    }
}

fun appendStr(bytes: ArrayList<Byte>, str: String) {
    str.toByteArray().forEach { bytes.add(it) }
}

fun consoleSearch(keyword: String, article: String) {
    val search = keyword
    val bc = BmMatch(search)
    val res = bc.match(article)
    println(res)
    var startIndex = 0
    val detailByteArray = article.toByteArray()
    var modifyByte = ArrayList<Byte>()
    val searchByteSize = search.toByteArray().size
    for (re in res) {
        for (i in startIndex until re) {
            modifyByte.add(detailByteArray[i])
        }
        appendStr(modifyByte, "\u001B[48;31;45m")
        appendStr(modifyByte, search)
        appendStr(modifyByte, "\u001B[1;0;0m")
        startIndex = re + searchByteSize
    }
    val lastIndex = res[res.size - 1]
    var endIndex = lastIndex + searchByteSize
    for (i in endIndex until article.toByteArray().size) {
        modifyByte.add(detailByteArray[i])
    }
    print(String(modifyByte.toByteArray()))
}

fun main(args: Array<String>) {
    val article = """前言
在绝大部分情况下我们在命令行终端或者控制台所输出的内容都是黑白色的，但是在使用 Spring Boot 与 IDEA 时却发现启动项目后在控制台竟然出现了彩色字体，那么这是这么实现的呢，其实就是用到了 ANSI 转义序列。

ANSI 转义序列
ANSI 转义序列（ANSI Escape codes）是一种带内信号（In-band signaling）的转义序列标准，用于控制视频文本终端上的光标位置、颜色和其他选项。在文本中嵌入确定的字节序列，大部分以 ESC 转义字符和 [ 字符开始，终端会把这些字节序列解释为相应的指令，而不是普通的字符编码。

固定格式
转移字符 Esc，ASCII 码为 27（十六进制：0x1b）
左中括号字符 [，ASCII 码为 91（十六进制：0x5b）
最后以字符m收尾
后跟控制键盘和显示功能的字母数字码（区分大小写）

Esc[Value;...;Valuem

支持设置的显示模式包括文本属性，前景色和背景色。

"""
    consoleSearch("信号",article)
    println("c郭".toByteArray().size)
}