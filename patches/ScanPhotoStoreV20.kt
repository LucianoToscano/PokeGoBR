package br.com.pokegobr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Optional user-selected screenshots. Images stay in internal app storage.
 * Never saved automatically, uploaded to a server, or placed in public storage.
 */
internal object ScanPhotoStore {
    private const val PREF = "pokegobr_scan_images"
    private const val DIR = "pokegobr_scan_previews"
    private val safeId = Regex("[a-zA-Z0-9-]{10,80}")

    fun currentPhotos(context: Context): Map<String, String> {
        val root = File(context.filesDir, DIR)
        val result = mutableMapOf<String, String>()
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).all
            .forEach { (id, name) ->
                if (safeId.matches(id) && name == "$id.jpg") {
                    val file = File(root, "$id.jpg")
                    if (file.isFile) result[id] = file.absolutePath
                }
            }
        return result
    }

    fun attach(context: Context, entry: SavedScan, uri: Uri) {
        if (!entry.own || !safeId.matches(entry.key))
            throw IllegalArgumentException("Só scans do aplicativo aceitam imagem")
        val source = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Não foi possível abrir a foto")
        val data = ByteArrayOutputStream()
        source.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                if (data.size() + count > 10 * 1024 * 1024)
                    throw IllegalArgumentException("Imagem acima de 10 MB")
                data.write(buffer, 0, count)
            }
        }
        val bytes = data.toByteArray()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth !in 1..12000 || options.outHeight !in 1..12000)
            throw IllegalArgumentException("Imagem não reconhecida")
        var sample = 1
        while (options.outWidth / sample > 1200 || options.outHeight / sample > 1600)
            sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("Falha ao ler captura")
        val directory = File(context.filesDir, DIR)
        if (!directory.exists() && !directory.mkdirs())
            throw IllegalStateException("Não foi possível criar pasta privada")
        val file = File(directory, entry.key + ".jpg")
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output))
                    throw IllegalArgumentException("Não foi possível salvar captura")
            }
        } finally { bitmap.recycle() }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(entry.key, file.name).apply()
    }

    fun delete(context: Context, id: String) {
        if (!safeId.matches(id)) return
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (prefs.getString(id, null) == "$id.jpg") {
            File(File(context.filesDir, DIR), "$id.jpg").delete()
            prefs.edit().remove(id).apply()
        }
    }
}
