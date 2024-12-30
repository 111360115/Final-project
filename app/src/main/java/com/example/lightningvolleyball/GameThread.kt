class GameThread(private val gameView: GameView) : Thread() {

    private var running: Boolean = true

    override fun run() {
        while (running) {
            // 更新遊戲邏輯
            gameView.updateGame()

            // 渲染畫面
            gameView.renderGame()

            // 控制遊戲的幀率
            try {
                sleep(16) // 60 FPS 大約每16毫秒更新一次
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    // 停止遊戲線程
    fun stopThread() {
        running = false
    }
}
