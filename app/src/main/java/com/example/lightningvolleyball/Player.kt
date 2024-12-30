import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Player(var x: Float, var y: Float, private val color: Int) {
    companion object {
        const val RADIUS = 50f
        const val SPEED = 15f
    }

    private val paint = Paint().apply {
        this.color = color
    }

    var velocityY = 0f
    var jumping = false

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, RADIUS, paint)
    }

    fun moveLeft() {
        x -= SPEED
    }

    fun moveRight() {
        x += SPEED
    }

    fun jump() {
        if (!jumping) {
            jumping = true
            velocityY = -30f
        }
    }

    fun update(gravity: Float, groundY: Float) {
        if (jumping) {
            y += velocityY
            velocityY += gravity
            if (y >= groundY) {
                y = groundY
                jumping = false
            }
        }
    }
}
