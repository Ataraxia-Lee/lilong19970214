package com.cloud.project.util

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.toJSONString
import com.google.common.base.Joiner
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.lang.Exception
import java.util.*

class M3u8Utils(file: File, websiteAddress: String) {
    private lateinit var list: List<String>
    private val websiteAddress: String = websiteAddress

    init {
        list = file.readLines().filter { it.contains(".ts") }
    }

    fun download() {
        val okHttpClient = OkHttpClient()
        if (list.isEmpty()) throw Exception("没有文件")
        list.forEach {
            val request = Request.Builder().get().url(websiteAddress + it).build()
            okHttpClient.newCall(request).execute().use { res ->
                val file = File("""f:/test/$it""")
                if (!file.exists()) file.createNewFile()
                file.writeBytes(res.body!!.bytes())
            }
        }
    }


}

fun concatTs(folder: File) {
    val numberList = LinkedList<File>()
    val list = folder.listFiles().toSet()
    for (item in 0..list.size) {
        list.find { it.name.replace("fileSequence", "").split(".")[0] == item.toString() }?.let { numberList.add(it) }
    }
    var command = Joiner.on("|").join(numberList)
    command = """ "concat:${command}" """

    println(command)
    val process = ProcessBuilder(
        "ffmpeg",
        "-i",
        command,
        "-c",
        "copy",
        "f:/output.mp4"
    )
    process.inheritIO().start().waitFor()
}


fun main() {
    val file = File("""D:\ren-py\renpy-8.0.3-sdk\the_question\game\tl\testChinese\script.rpy""")
    //定义存放文本的集合，泛型中的逻辑是行号加内容。方便我们反推将内容回填回去
    val text = mutableListOf<Pair<Int, String>>()
    file.useLines { lines ->
        lines.forEachIndexed { index, s ->
            text.add(Pair(index, s))
        }
    }
    //将需要翻译的文本进行正则选择然后进行处理【批量翻译Api 又字符限制，对集合进行分割每次只提取10条对话进行翻译】
    val list = text.filter { it.second.matches("^\\s{4}#.*".toRegex()) && it.second.contains("\"") }
        .map {
            println(it.second)
            it.first to it.second.substring(it.second.indexOf("\"") + 1, it.second.lastIndexOf("\""))
        }
        .chunked(10)

    println(list)

    val textContent = mutableListOf<Pair<Int, String>>()
    for (it in list) {
        val translateText = BatchV3Demo.translateBatch(it.map { it.second }.toTypedArray())
        val translateResultsArray = JSONObject.parseObject(translateText).getJSONArray("translateResults")
        for ((index, item) in translateResultsArray.withIndex()) {
            val rowNumber = it[index].first
            val translation = JSONObject.parseObject(item.toJSONString()).getString("translation")
            //替换内容
            textContent.add(Pair(rowNumber, translation))
        }
    }

    replaceContent(textContent)

}

/*替换文本内容*/
fun replaceContent(textContent: List<Pair<Int, String>>) {
    val file = File("""D:\ren-py\renpy-8.0.3-sdk\the_question\game\tl\testChinese\script.rpy""")
    //读取行
    val list = mutableListOf<String>()
    file.useLines { lines ->
        lines.forEach { list.add(it) }
    }
    //替换指定行的内容
    textContent.forEach {
        val rowNumber = it.first
        val value = it.second
        list[rowNumber + 1] = "\u00A0\u00A0\u00A0\u00A0\"$value\""
    }

    val testFile = File("""C:\Users\MyPC\Desktop\\test.txt""")
    testFile.writeText("")
    list.forEach {
        testFile.appendText("$it\r\n")
    }
}
