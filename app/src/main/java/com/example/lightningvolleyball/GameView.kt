import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    init {
        // 註冊 SurfaceHolder.Callback
        val holder: SurfaceHolder = holder
        holder.addCallback(this)
    }

    // 實作 surfaceChanged 方法
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 可以在這裡更新遊戲視窗大小
    }

    // 實作 surfaceCreated 方法
    override fun surfaceCreated(holder: SurfaceHolder) {
        // 初始化遊戲狀態，啟動 GameThread
        val gameThread = GameThread(this)
        gameThread.start()
    }

    // 實作 surfaceDestroyed 方法
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // 在這裡處理 Surface 銷毀時的操作
    }

    // 更新遊戲的邏輯，這是你要的 updateGame 方法
    fun updateGame() {
        // 在這裡執行遊戲更新邏輯，例如移動球、玩家行為等
        // 例如：更新畫面、球的移動、玩家的控制
        // 這裡僅為示例，具體邏輯需要根據你的需求來實作
    }

    // 渲染畫面，繪製遊戲畫面
    fun renderGame() {
        val canvas: Canvas = holder.lockCanvas()
        // 在這裡繪製遊戲畫面
        // 比如繪製球和玩家
        holder.unlockCanvasAndPost(canvas)
    }
}
