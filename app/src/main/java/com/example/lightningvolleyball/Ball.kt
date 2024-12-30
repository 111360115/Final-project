import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    companion object {
        const val RADIUS = 30f
    }

    private val paint = Paint().apply {
        color = Color.BLUE
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, RADIUS, paint)
    }

    fun update() {
        x += vx
        y += vy
    }

    fun checkBoundaryCollision(width: Float, height: Float) {
        if (x <= RADIUS || x >= width - RADIUS) {
            x = x.coerceIn(RADIUS, width - RADIUS)
            vx = -vx
        }
        if (y <= RADIUS) {
            y = RADIUS
            vy = -vy
        }
    }

    fun checkPlayerCollision(playerX: Float, playerY: Float, playerRadius: Float): Boolean {
        val dx = x - playerX
        val dy = y - playerY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
        return distance <= (RADIUS + playerRadius)
    }

    fun resetPosition(startX: Float, startY: Float) {
        x = startX
        y = startY
        vx = 0f
        vy = 0f
    }
}
