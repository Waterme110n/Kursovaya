import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class ImageData(
    val base64Image: String,
    val tags: List<String> = emptyList(),
    val title: String,
    val description: String,
    val creatorId: String,
    var like: List<String> = emptyList(),
    var dislike: List<String> = emptyList(),
    val comments: List<Pair<String, String>> = emptyList()
)

class ImageHandler {

    fun decodeBase64ToImages(base64String: String): List<Bitmap> {
        val decodedBytes: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
        return listOf(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size))
    }


    fun encodeImageToBase64FromResourse(context: Context, resId: Int): String {

        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, resId)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }


    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArray = bitmapToByteArray(bitmap)
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun uploadImageToFirestore(originalBitmap: Bitmap, tags: List<String>, title: String, desc: String) {
        // Устанавливаем желаемую ширину
        val targetWidth = 400 // Замените на нужное значение

        // Получаем оригинальные размеры изображения
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // Вычисляем новую высоту с сохранением пропорций
        val newHeight = (originalHeight * (targetWidth.toFloat() / originalWidth)).toInt()

        // Изменяем размер изображения
        val resizedBitmap = resizeBitmap(originalBitmap, targetWidth, newHeight)
        val encodedImage = encodeImageToBase64(resizedBitmap)

        // Получаем идентификатор текущего пользователя
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""

        val imageData = ImageData(
            base64Image = encodedImage,
            tags = tags,
            title = title,
            description = desc,
            creatorId = creatorId,

        )

        val db = FirebaseFirestore.getInstance()
        db.collection("images").add(imageData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Document added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }

    fun downloadImage(base64Image: String, context: Context) {
        val decodedBytes: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(picturesDir, "downloaded_image_${System.currentTimeMillis()}.png")

        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }

            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)

            Toast.makeText(context, "Image successfully downloaded ", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
        }
    }

}