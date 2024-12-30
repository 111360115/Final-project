import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint

class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    private val paint = Paint()

    init {
        paint.color = Color.WHITE
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, 30f, paint)
    }

    fun update() {
        x += vx
        y += vy

        // 簡單的邊界碰撞檢測
        if (x < 30 || x > 1080 - 30) vx = -vx
        if (y < 30 || y > 1920 - 30) vy = -vy
    }
}
