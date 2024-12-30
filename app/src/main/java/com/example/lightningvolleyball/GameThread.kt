class GameThread(private val gameView: GameView) : Thread() {
    private var running = true

    override fun run() {
        while (running) {
            gameView.updateGame()
            gameView.renderGame()

            try {
                sleep(16) // 60 FPS
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun stopThread() {
        running = false
    }
}
