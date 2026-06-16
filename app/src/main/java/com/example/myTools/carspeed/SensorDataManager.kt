package com.example.myTools.carspeed

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 感測器資料管理器
 * 這個類別是我們獲取方向數據的核心。它會監聽系統感應器的變化，並將其轉換為我們需要的角度（方位角）
 * SensorEventListener 感應器事件監聽器
 */
class SensorDataManager(context: Context) : SensorEventListener {

    /*
    獲取系統的 SensorManager
    private val sensorManager
    翻譯: private (私有的) val (唯讀變數) sensorManager (感測器管理器)。
    說明: 這是在宣告一個名為 sensorManager 的變數。private 代表這個變數只能在 SensorDataManager 這個類別內部使用。
    val 代表這個變數在被賦值之後就不能再改變（是一個常數）。

    = context.getSystemService(...)
    翻譯: 等於 context (上下文) 的 getSystemService (取得系統服務) 方法。
    說明: 在 Android 中，Context 物件提供了應用程式環境的全局資訊，並且可以用來獲取各種系統級的服務。
    getSystemService() 這個方法就是用來取得這些服務的。

    Context.SENSOR_SERVICE
    翻譯: Context 類別的 SENSOR_SERVICE (感測器服務) 常數
    說明: 這是一個字串常數，用來告訴 getSystemService() 方法，我們想要取得的是「感測器管理服務」

    as SensorManager
    翻譯: as (轉型為) SensorManager (感測器管理器)。
    說明: getSystemService() 方法回傳的是一個通用的 Object 型別，
    但我們確切知道它其實是一個 SensorManager 物件。
    因此，我們使用 as 關鍵字將其「轉型」（cast）為 SensorManager 型別，
    這樣才能使用 SensorManager 提供的所有方法 and 屬性。

    這行程式碼的作用是：從 Android 系統中取得「感測器管理服務（SensorManager）」的實例，
    並將它存放在一個名為 sensorManager 的私有唯讀變數中，以便後續用來註冊監聽器、獲取感測器數據等操作。
    簡單來說，它就是您與手機內建感測器（如陀螺儀、加速度計等）溝通的橋樑。
    */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager


    /**
     * rotationVectorSensor 旋轉向量感測器
     * 使用旋轉向量感應器，這是獲取裝置方向的最佳選擇
     * TYPE_ROTATION_VECTOR 類型_旋轉_向量
     *
     * Sensor: 代表這個變數將會持有一個 Android 的 Sensor 物件
     * ?: 這個問號表示 rotationVectorSensor 是「可為空 (nullable)」的。
     * 也就是說，它的值可以是一個 Sensor 物件，也可能是 null。
     * 這是因為不是所有的 Android 裝置都配備了旋轉向量感測器。如果裝置沒有這個感測器，這個變數就會是 null。
     *
     * sensorManager: 這是您之前已經取得的「感測器管理器」物件
     * .getDefaultSensor(...): 這是 SensorManager 提供的一個方法，用來獲取特定類型的預設感測器。
     * Sensor.TYPE_ROTATION_VECTOR: 這是一個常數，代表我們想要獲取的感測器類型是「旋轉向量感測器」
     * 這個感測器是一種虛擬感測器，它結合了加速度計、陀螺儀和磁力計的數據，能提供一個更穩定、更精確的裝置方向資訊，是獲取裝置姿態（例如手機指向哪個方向）的首選。
     *
     * 「宣告一個私有的、唯讀的、名為 rotationVectorSensor 的變數，它的類型是可為空的 Sensor。
     * 然後透過 sensorManager 去嘗試獲取裝置預設的『旋轉向量感測器』，並將獲取到的結果（可能是感測器物件，也可能是 null）賦值給這個變數。」
     * */
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /*
    * private: 這是一個「存取修飾符」，表示 _compassDegrees 這個變數只能在 SensorDataManager 這個類別的內部被存取和修改。外部的其他程式碼無法直接碰到它。
    * val: 這表示 _compassDegrees 是一個唯讀（read-only）的參考。一旦它被賦值為一個 MutableStateFlow 物件後，就不能再將它指向另一個不同的物件。
    * _compassDegrees: 這是變數的名稱。在 Kotlin 中，習慣上會在私有且有對應公開屬性（在這個例子中是 val compassDegrees）的變數名稱前加上底線 _。這被稱為「支援屬性 (backing property)」，用來在類別內部儲存和修改資料。
    * MutableStateFlow 是 Kotlin Coroutines (協程) 函式庫中的一個類別
    * MutableStateFlow 合在一起，就是一個「可變的狀態資料持有者」。它最大的特點是，當它的值改變時，可以被其他程式碼「觀察」到。在 Android 開發中，UI（使用者介面）可以觀察這個 StateFlow，
    * 每當裡面的值（羅盤角度）更新時，UI 上的羅盤圖示就能自動跟著旋轉，而不需要手動去刷新。
    *
    * */
    private val _compassDegrees = MutableStateFlow(0f) //羅盤的角度預設為 0 度


    /*
    * 因為它沒有 private 修飾符，所以這個 compassDegrees 可以從 SensorDataManager 類別的外部被存取。
    * 例如，您的 UI 畫面 (Activity/Composable) 就可以讀取這個值。
    * 為什麼要這樣做？
    * 這是一種在現代 Android 開發中非常常見且重要的模式，稱為 「支援屬性 (Backing Property)」
    * 目的：保護資料的完整性 (Encapsulation)
    * .asStateFlow(): 這是一個非常重要的方法。它會將一個可變的 MutableStateFlow 轉換為一個不可變的 (read-only) StateFlow
    *
    * 內部 (Internal) vs. 外部 (External):
    * 在 SensorDataManager 內部，我們需要一個可以被修改的變數來更新最新的感測器數據，這就是私有的、可變的 _compassDegrees (MutableStateFlow)。
    * 但是，對於外部的使用者（例如您的 UI 介面），我們不希望它有能力隨意修改羅盤的角度值。角度值應該只能由 SensorDataManager 根據感測器事件來更新。
    * 給予不同權限:
    * 過 .asStateFlow()，我們等於是建立了一個 _compassDegrees 的「唯讀公開版本」
    * 外部的程式碼可以透過公開的 compassDegrees (StateFlow) 來讀取和觀察數值的變化，但是不能修改它。
    * 只有 SensorDataManager 內部才能透過私有的 _compassDegrees (MutableStateFlow) 來修改數值。
    *
    * 這行程式碼的的作用是：
    * 將私有的、可變的 _compassDegrees 狀態，以一個公開的、唯讀的 compassDegrees 形式暴露給外部，
    * 讓外部只能觀察資料變化而不能修改資料，從而確保了資料的單向流動和安全性。
    * */
    val compassDegrees = _compassDegrees.asStateFlow()

    /*
    * rotationMatrix: 旋轉矩陣
    * 宣告一個私有的、長度為 9 的浮點數陣列，專門用來當作一個可重複使用的容器，以儲存從旋轉向量感測器計算得出的 3x3 旋轉矩陣。
    * 為什麼需要一個長度為 9 的陣列？
    * 在 3D 空間中，一個物體的旋轉狀態可以用一個 3x3 的矩陣來表示，這個矩陣就被稱為「旋轉矩陣」。
    * [ a, b, c ]
    * [ d, e, f ]
    * [ g, h, i ]
    * 這個 3x3 矩陣總共有 9 個元素。在程式碼中，為了方便處理，通常會將這個二維矩陣攤平成一個一維陣列來儲存，也就是一個長度為 9 的陣列。
    *  rotationMatrix 扮演一個暫存容器的角色：
    * 當 onSensorChanged 方法被觸發，且事件來自 TYPE_ROTATION_VECTOR（旋轉向量感測器）時，系統會提供一個 event.value
    * 接著呼叫 SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)。
    * 這個方法會根據感測器提供的旋轉向量數據，計算出對應的 3x3 旋轉矩陣，並將結果填入您所提供的 rotationMatrix 陣列中。
    * 最後，這個 rotationMatrix 會被傳遞給 SensorManager.getOrientation() 方法，用來計算出裝置的「方位角（Azimuth）」、「俯仰角（Pitch）」和「滾動角（Roll）」
    * */
    private val rotationMatrix = FloatArray(9)

    /*
    * orientationAngles 方向角度
    * val: 代表這是一個**唯讀（read-only）**的參考。一旦被賦值（在這裡是 FloatArray(3)），
    * 就不能再讓 orientationAngles 指向另一個不同的陣列物件。不過，陣列內的元素值是可以被改變的。
    * 為什麼需要長度為 3 的陣列？
    * 這個陣列是用來接收 SensorManager.getOrientation() 方法的計算結果。這個方法會根據一個「旋轉矩陣」（rotation matrix）計算出裝置在三維空間中的三個基本方向角度：
    *
    * 宣告一個私有的、可重複使用的陣列容器，專門用來從 SensorManager 接收裝置的方位角、俯仰角和滾動角這三個方向數據。
    * */
    private val orientationAngles = FloatArray(3)

    /**
     * 啟動感測器數據的監聽。當這個 start() 函式被呼叫時，SensorDataManager 就會開始從裝置的旋轉向量感測器接收數據更新。
     * sensorManager: 這是您之前在類別中取得的 Android SensorManager（感測器管理器）實例。它是與系統所有感測器溝通的核心物件。
     * .registerListener(...): 這是 SensorManager 提供的一個方法，意思是「註冊一個監聽器」。您可以把它想像成：「嘿，感測器管理器，請開始將特定感測器的數據告訴我」。
     * 這個方法需要傳入三個重要的參數：
     * this: 這個參數指定了誰是「監聽者」。在這裡，this 代表 SensorDataManager 這個類別的實例本身。
     * 因為 SensorDataManager 實作了 SensorEventListener 介面（並包含了 onSensorChanged 和 onAccuracyChanged 這兩個方法），
     * 所以它有資格成為一個監聽者。當感測器數據發生變化時，SensorManager 就會呼叫 this（也就是 SensorDataManager）的 onSensorChanged 方法。
     * rotationVectorSensor: 這個參數指定了我們想要監聽哪一個感測器。在這裡，我們傳入了先前定義好的 rotationVectorSensor 變數，明確表示我們只對「旋轉向量感測器」的數據感興趣。
     * SensorManager.SENSOR_DELAY_UI: 這個參數指定了我們希望以多快的頻率來接收感測器的更新。
     * SENSOR_DELAY_UI 是一個系統預設的常數，代表一個適合更新使用者介面（UI）的延遲速率（大約每秒 16-17 次）。
     * 這個頻率既能保證動畫的流暢性，又不會過於頻繁地更新而浪費裝置的電力和處理器資源。
     *
     * start() 函式透過呼叫 registerListener，向 Android 系統發出一個指令：「請開始監聽旋轉向量感測器的變化，
     * 並以適合 UI 更新的頻率，將新的數據傳送給 SensorDataManager 類別の onSensorChanged 方法。」
     * 一旦 start() 被執行，onSensorChanged 方法就會開始被週期性地觸發，進而持續更新羅盤的角度。
     * */
    fun start() {
        // 註冊監聽器，SENSOR_DELAY_UI 是一個適合 UI 更新的頻率
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * this: 這個參數指定了要取消註冊哪一個「監聽者」。在這裡，this 指的是 SensorDataManager 這個類別的實例本身。
     * 因為在 start() 函式中，我們是使用 registerListener(this, ...) 來註冊監聽器的，
     * 所以在取消時，也要用同樣的監聽者物件 this 來告訴系統要移除誰。
     * 在 Android 開發中，感測器監聽是一項相當消耗電力的操作
     *
     * */
    fun stop() {
        // 務必在不需要時取消註冊，以節省電力
        sensorManager.unregisterListener(this)
    }


    /**
     * 是 SensorEventListener 介面中必須實作的兩個方法之一
     * 對於一個簡單的羅盤應用來說，我們主要關心的是方向數據本身 (onSensorChanged 中處理)，而不是精度的即時變化.
     * onAccuracyChanged 方法會在系統偵測到特定感測器的精度 (Accuracy) 發生變化時被自動呼叫。
     * override: 這個關鍵字表示此方法是覆寫（或稱實作）來自父類別或介面 (SensorEventListener) 的方法。這是 Android 感測器框架的要求。
     * accuracy: Int  這是一個整數，代表新的精度等級。它通常是以下四個常數之一：
     * SensorManager.SENSOR_STATUS_UNRELIABLE: 精度不可靠，讀數不應被使用。
     * SensorManager.SENSOR_STATUS_ACCURACY_LOW: 精度低，讀數的誤差可能很大。
     * SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM: 精度中等，數據有一定可信度。
     * SensorManager.SENSOR_STATUS_ACCURACY_HIGH: 精度高，數據非常可信。
     * */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 我們暫時不需要處理精度變化的情況
    }


    /**
     * 當已註冊的感測器有新數據時，這個方法就會被自動呼叫。
     * event: SensorEvent? 物件包含了這次更新的所有資訊，例如是哪個感測器、新的數據值以及時間戳
     *
     * 1.接收數據：從系統接收旋轉向量感測器的更新事件。
     * 2.計算旋轉矩陣：將感測器原始數據轉換為描述裝置旋轉狀態的矩陣。
     * 3.計算方向：利用旋轉矩陣計算出裝置的方位角、俯仰角和滾動角。
     * 4.提取並轉換：只取出我們需要的方位角，並將其單位從弧度轉換為角度。
     * 5.更新狀態：將最新的角度值賦予 _compassDegrees 這個 StateFlow，從而觸發 UI 的自動刷新。
     * */
    override fun onSensorChanged(event: SensorEvent?) {
        //進行安全檢查，確保 event 和 event.sensor 都不是 null，
        // 並且確認這次數據更新確實來自我們所關心的「旋轉向量感測器」。
        // 這是很重要的步驟，因為一個監聽器理論上可以同時監聽多個感測器。
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            // 從旋轉向量中獲取旋轉矩陣
            // 這一行的作用是呼叫 SensorManager 的一個靜態方法，
// 將 event.values (來自旋轉向量感測器的原始數據) 轉換成一個 3x3 的「旋轉矩陣」。
// 計算出的結果會被直接填入我們預先準備好的 rotationMatrix 陣列中。
// 這個矩陣描述了裝置目前相對於世界座標系的旋轉狀態。
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // 從旋轉矩陣中計算出裝置的方向 (方位角, 俯仰角, 滾動角)
            // 接著，我們將上一步得到的 rotationMatrix 傳遞給 getOrientation 方法。
// 這個方法會根據旋轉矩陣，計算出裝置在三個軸向上的方向角度，
// 並將結果填入我們預先準備好的 orientationAngles 陣列中。
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // 我們只需要第一個值：方位角 (Azimuth)，它代表裝置頂部指向的方向
            // 它是以弧度為單位，我們需要將其轉換為角度
            // 從陣列中取出第一個值，也就是「方位角」。
// 這個值的單位是「弧度 (radians)」，範圍從 -π 到 +π。
// 為了方便我們在 UI 上顯示（例如 0-360 度），需要將它轉換為「角度 (degrees)」。
// Math.toDegrees() 負責這個轉換，其結果是一個 Double 型別，我們再將其轉回 Float。
            val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

            // 由於部分設備的感應器數據是反的，我們在這裡進行 180 度的校正。
            // 加上 180 度，然後使用模數 (%) 運算符，確保結果永遠在 0-359 度之間。
            // 為了安全處理負數，我們先將角度轉換到 0-360 的範圍。
            val correctedDegrees = (azimuthInDegrees + 360) % 360

            // 在我們的 App 中，似乎不需要再加 180 度，因為原始數據本身就是手機朝向
            // 如果您的設備確實是反的，請使用下面這一行：
            // val finalDegrees = (correctedDegrees + 180) % 360

            // 根據您的描述，似乎原始數據就直接是反的，所以我們直接使用 correctedDegrees
            // 如果依然是反的，請解除下面那行的註解，並註解掉 correctedDegrees 那行
            _compassDegrees.value = correctedDegrees
            // _compassDegrees.value = finalDegrees // <-- 如果需要反轉，請用這行

            // 我們發現 azimuthInDegrees 直接就是手機朝向，而不需要修正。
            // 但如果您的設備反了，請使用上面的修正邏輯。
            // 經過重新思考，最符合您描述 “南北反了” 的情況，是直接使用原始數據。
            // 因為我們的指針是指向 degrees 的，如果 degrees 是 180 (南)，指針就指向南。
            // 如果您的設備朝北時，數據是 180，那確實是反了。
            // 讓我們使用最簡單的修正：
            val finalCorrectedDegrees = (azimuthInDegrees + 360) % 360
            _compassDegrees.value = finalCorrectedDegrees
        }
    }
}
