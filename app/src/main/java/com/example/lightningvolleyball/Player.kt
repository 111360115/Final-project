import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color


class Player(var x: Float, var y: Float) {
    private val paint = Paint()

    init {
        paint.color = Color.YELLOW
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, 50f, paint) // 畫出角色（圓形）
    }

    fun moveLeft() {
        x -= 20f
    }

    fun moveRight() {
        x += 20f
    }

    fun jump() {
        y -= 100f
    }

    fun fall() {
        y += 10f
    }
}
