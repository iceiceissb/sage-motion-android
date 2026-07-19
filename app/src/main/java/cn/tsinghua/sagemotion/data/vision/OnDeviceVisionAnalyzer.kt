package cn.tsinghua.sagemotion.data.vision

import android.content.Context
import android.net.Uri
import cn.tsinghua.sagemotion.model.VisionFinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class OnDeviceVisionAnalyzer(private val context: Context) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.35f)
            .build(),
    )

    fun analyze(
        uri: Uri,
        onSuccess: (List<VisionFinding>) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val image = runCatching { InputImage.fromFilePath(context, uri) }
            .getOrElse { error -> onFailure(error); return }
        labeler.process(image)
            .addOnSuccessListener { labels ->
                onSuccess(
                    labels
                        .sortedByDescending { it.confidence }
                        .take(5)
                        .map { VisionFinding(localizeLabel(it.text), it.confidence) },
                )
            }
            .addOnFailureListener(onFailure)
    }

    fun close() = labeler.close()

    private fun localizeLabel(label: String): String = LABEL_TRANSLATIONS[label] ?: label

    private companion object {
        val LABEL_TRANSLATIONS = mapOf(
            "Flower" to "花朵",
            "Plant" to "植物",
            "Petal" to "花瓣",
            "Garden" to "花园",
            "Park" to "公园",
            "Tree" to "树木",
            "Leaf" to "叶片",
            "Nature" to "自然",
            "Lake" to "湖面",
            "Sky" to "天空",
            "Person" to "人物",
            "People" to "人群",
            "Animal" to "动物",
            "Dog" to "狗",
            "Cat" to "猫",
            "Bird" to "鸟",
            "Food" to "食物",
            "Fruit" to "水果",
            "Building" to "建筑",
            "Architecture" to "建筑结构",
            "Vehicle" to "交通工具",
            "Car" to "汽车",
            "Bicycle" to "自行车",
            "Water" to "水域",
            "Bridge" to "桥",
            "Sculpture" to "雕塑",
            "Furniture" to "家具",
        )
    }
}
