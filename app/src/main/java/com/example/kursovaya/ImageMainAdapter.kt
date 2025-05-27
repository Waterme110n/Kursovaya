package com.example.kursovaya

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView

class ImageMainAdapter(
    private var images: List<Pair<String, String>>,
    private val leftColumn: LinearLayout,
    private val rightColumn: LinearLayout,
    private val isCreatedButtonActive: Boolean,
    private val isAdmin: Boolean) {


    fun bind() {
        leftColumn.removeAllViews()
        rightColumn.removeAllViews()

        for (i in images.indices) {
            val cardView = LayoutInflater.from(leftColumn.context)
                .inflate(R.layout.item_main, null) as CardView
            val imageView = cardView.findViewById<ImageView>(R.id.imageView)

            // Декодируем изображение
            val imageBytes = Base64.decode(images[i].second, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)

            cardView.setOnClickListener {
                val context = leftColumn.context
                val intent = Intent(context, if (isCreatedButtonActive) ImageEditActivity::class.java else ImageInfoActivity::class.java).apply {
                    putExtra("imageId", images[i].first)
                    putExtra("isAdmin", isAdmin)
                }
                context.startActivity(intent)
            }

            val marginInDp = 3
            val scale = leftColumn.context.resources.displayMetrics.density
            val marginInPx = (marginInDp * scale + 0.5f).toInt()

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, marginInPx, 0, marginInPx)
            cardView.layoutParams = params

            if (i % 2 == 0) {
                leftColumn.addView(cardView)
            } else {
                rightColumn.addView(cardView)
            }
        }
    }
}