import com.google.gson.Gson
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest


object FingerprintUtils {
    /**
     *     var SHA1 = "SHA1"
     *     var SHA256 = "SHA-256"
     *    var MD5 = "MD5"
     * */

    fun calcFileFingerprint(file: File, algorithm: String?): String? {
        if (!file.isFile) {
            return null
        }
        val digest: MessageDigest?
        val `in`: FileInputStream?
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance(algorithm)
            `in` = FileInputStream(file)
            while (`in`.read(buffer, 0, 1024).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        val bigInt = BigInteger(1, digest.digest())
        return fixTo40(bigInt)
    }

    private fun fixTo40(bigInt: BigInteger): String {
        val s = StringBuilder(bigInt.toString(16))
        val length = s.length
        for (j in 0 until 40 - length) {
            s.insert(0, "0")
        }
        return s.toString()
    }

}

data class Finger(val name: String, val finger: String)
class Fingers constructor(var finger: Map<String, Finger>)

fun calcFinger(files: Collection<File>): Map<String, Finger> {
    val fingers = HashMap<String, Finger>()
    files.forEach {
        val calcFileFingerprint = FingerprintUtils.calcFileFingerprint(it, "MD5")
        calcFileFingerprint?.apply {
            val finger = Finger(it.absolutePath, this)
            println("$finger")
            fingers[it.absolutePath] = finger
        }
    }
    return fingers
}

data class ChangelogFile(
    val add: MutableCollection<File>?,
    val update: List<File>?,
    val nc: List<File>?,
    val delete: List<Finger>?
)

fun listModifyFile(fp: String, mds: Collection<File>): ChangelogFile {
    val gson = Gson()
    val docFingers = calcFinger(mds)
    val modifyCache = ".okmodify"
    val fingerCache = File(fp, modifyCache)
    val fileSite = HashMap<String, File>()
    mds.forEach { fileSite[it.absolutePath] = it }
    val addFiles = ArrayList<File>()
    val updateFiles = ArrayList<File>()
    val deleteFiles = ArrayList<Finger>()
    val ncFiles = ArrayList<File>()
    val changeFinger = HashMap<String, Finger>()
    if (fingerCache.exists()) {
        val readFileToString = FileUtils.readFileToString(fingerCache)
        val oldFingers = gson.fromJson(readFileToString, Fingers::class.java).finger
        //当前文档写入文件
        FileUtils.writeLines(File(fp, modifyCache), arrayListOf(gson.toJson(Fingers(docFingers))))
        docFingers.forEach { (t, u) ->
            if (!oldFingers.containsKey(t)) {
                changeFinger[t] = u
                fileSite[t]?.apply { addFiles.add(this) }
            } else {
                oldFingers[t]?.apply {
                    if (this.finger != u.finger) {
                        changeFinger[t] = this
                        fileSite[t]?.apply { updateFiles.add(this) }
                    } else {
                        fileSite[t]?.apply { ncFiles.add(this) }
                    }
                }
            }
        }
        oldFingers.forEach { (t, u) ->
            if (docFingers[t] == null) {
                deleteFiles.add(u)
            }
        }
        return ChangelogFile(addFiles, updateFiles, ncFiles, deleteFiles)
    } else {
        FileUtils.writeLines(File(fp, modifyCache), arrayListOf(gson.toJson(Fingers(docFingers))))
        return ChangelogFile(fileSite.values, null, null, null)
    }
}