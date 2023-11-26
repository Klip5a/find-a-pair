package com.example.pairup

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.pairup.R
import com.yandex.mobile.ads.common.AdError
import kotlin.math.floor
import kotlin.random.Random
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener

class MainActivity : AppCompatActivity() {

    // Контейнеры для элементов интерфейса
    private lateinit var setupContainer: View
    private lateinit var gameContainer: GridLayout
    private lateinit var restartButton: Button
    private lateinit var gameMessage: TextView
    private lateinit var timerDisplay: TextView
    private lateinit var backButton: Button

    // Переменные для управления игрой
    private var difficulty: Int = 0
    private var isGameInProgress = false
    private var isClickProcessing = false
    private var isDialogVisible = false
    private lateinit var timer: CountDownTimer
    private var timeInSeconds = 30
    private var pairCount = 0
    private var interstitialAd: InterstitialAd? = null
    private var interstitialAdLoader: InterstitialAdLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        setupContainer = findViewById(R.id.setupContainer)
        gameContainer = findViewById(R.id.gameContainer)
        restartButton = findViewById(R.id.restartButton)
        gameMessage = findViewById(R.id.gameMessage)
        timerDisplay = findViewById(R.id.timerDisplay)
        backButton = findViewById(R.id.backButton)


        // Инициализация кнопок выбора сложности
        val easyButton: Button = findViewById(R.id.easyButton)
        val mediumButton: Button = findViewById(R.id.mediumButton)
        val hardButton: Button = findViewById(R.id.hardButton)
        val expertButton: Button = findViewById(R.id.expertButton)

        // Установка слушателей для кнопок выбора сложности
        easyButton.setOnClickListener { startGame(4) }
        mediumButton.setOnClickListener { startGame(6) }
        hardButton.setOnClickListener { startGame(8) }
        expertButton.setOnClickListener { startGame(10) }

        // Установка слушателя для кнопки перезапуска игры
        restartButton.setOnClickListener {
            resetGame()
            hideDialog() // Скрываем диалоговое окно при нажатии "Еще раз"

            if (interstitialAd != null) {
                showInterstitialAd()
            } else {
                // Ad is not loaded, or interstitialAd is null
                // You can handle this case based on your application's logic
                // For example, you might want to load a new interstitial ad or take another action.
                loadInterstitialAd()
            }
        }

        // Установка слушателя для кнопки "Назад"
        backButton.setOnClickListener { returnToMainMenu() }

        // Инициализация рекламы
        initAds()

        // Загрузка межстраничного объявления
        loadInterstitialAd()
    }

    private fun initAds() {
        interstitialAdLoader = InterstitialAdLoader(this).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    this@MainActivity.interstitialAd = interstitialAd
                    // Реклама успешно загружена. Теперь вы можете показать загруженное объявление.
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    // Ошибка загрузки рекламы с AdRequestError.
                    // Попытка загрузки новой рекламы из метода onAdFailedToLoad() крайне не рекомендуется.
                }
            })
        }
    }

    private fun loadInterstitialAd() {
        val adRequestConfiguration = AdRequestConfiguration.Builder("R-M-4054253-4").build()
        interstitialAdLoader?.loadAd(adRequestConfiguration)
    }

    // Добавьте метод для показа межстраничного объявления
    private fun showInterstitialAd() {
        interstitialAd?.apply {
            setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    // Вызывается, когда объявление показано
                }

                override fun onAdFailedToShow(adError: AdError) {
                    // Вызывается, если произошла ошибка при показе объявления
                }

                override fun onAdDismissed() {
                    // Вызывается, когда объявление закрыто
                    // Освобождаем ресурсы после закрытия объявления
                    interstitialAd?.setAdEventListener(null)
                    interstitialAd = null

                    // Теперь вы можете предварительно загрузить следующее межстраничное объявление
                    loadInterstitialAd()
                }

                override fun onAdClicked() {
                    // Вызывается, когда зафиксирован клик по объявлению
                }

                override fun onAdImpression(impressionData: ImpressionData?) {
                    // Вызывается при записи впечатления от объявления
                }
            })
            show(this@MainActivity)
        }
    }


    // Метод для обновления отображения таймера
    private fun updateTimerDisplay() {
        timerDisplay.visibility = View.VISIBLE
        val timeLeftText = getString(R.string.time_left, timeInSeconds)
        timerDisplay.text = timeLeftText

        if (timeInSeconds <= 0) {
            timer.cancel()
            endGame()
        } else {
            if (::timer.isInitialized) {
                timer.cancel()
            }

            timer = object : CountDownTimer((timeInSeconds * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeInSeconds--
                    val updatedTimeLeftText = getString(R.string.time_left, timeInSeconds)
                    timerDisplay.text = updatedTimeLeftText
                }

                override fun onFinish() {
                    endGame()
                }
            }.start()
        }
    }

    // Ленивая инициализация переменной для отступа между картами
    private val marginBetweenCards: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.margin_between_cards)
    }

    // Метод для создания списка чисел для пар карт
    private fun createNumbersArray(pairCount: Int): MutableList<Int> {
        val numbers = mutableListOf<Int>()
        for (i in 1..pairCount) {
            numbers.add(i)
            numbers.add(i)
        }
        return numbers
    }

    // Метод для создания отдельной карты
    private fun createCard(number: Int): View {
        val parent = findViewById<ViewGroup>(R.id.cardContainer) // Replace with the actual ID of the parent layout
        val card = layoutInflater.inflate(R.layout.card_layout, parent, false)
        val cardText: TextView = card.findViewById(R.id.cardText)

        card.setBackgroundResource(R.drawable.card_button_background)
        cardText.text = ""
        card.tag = "closed"

        card.setOnClickListener {
            handleCardClick(card, number)
        }

        return card
    }

    // Метод для обработки клика по карте
    private fun handleCardClick(card: View, number: Int) {
        if (isClickProcessing) {
            return
        }

        isClickProcessing = true

        val state = card.tag as String
        val cardText = card.findViewById<TextView>(R.id.cardText)

        if (state == "closed") {
            card.tag = "opened"
            card.setBackgroundResource(R.drawable.card_button_opened_background)
            cardText.text = number.toString()
            cardText.visibility = View.VISIBLE

            val openedCards = mutableListOf<View>()
            for (i in 0 until gameContainer.childCount) {
                val child = gameContainer.getChildAt(i)
                if (child.tag == "opened") {
                    openedCards.add(child)
                }
            }

            if (openedCards.size == 2) {
                val (firstCard, secondCard) = openedCards

                Handler(mainLooper).postDelayed({
                    if (firstCard.tag == secondCard.tag &&
                        firstCard.findViewById<TextView>(R.id.cardText).text == secondCard.findViewById<TextView>(
                            R.id.cardText
                        ).text
                    ) {
                        // Карты совпали
                        firstCard.setBackgroundResource(R.drawable.card_button_matched_background)
                        secondCard.setBackgroundResource(R.drawable.card_button_matched_background)
                        firstCard.tag = "matched"
                        secondCard.tag = "matched"
                        checkGameCompletion()
                    } else {
                        // Карты не совпали
                        firstCard.tag = "closed"
                        secondCard.tag = "closed"
                        firstCard.setBackgroundResource(R.drawable.card_button_background)
                        secondCard.setBackgroundResource(R.drawable.card_button_background)
                        firstCard.findViewById<TextView>(R.id.cardText).visibility = View.INVISIBLE
                        secondCard.findViewById<TextView>(R.id.cardText).visibility = View.INVISIBLE
                    }

                    isClickProcessing = false
                }, 500)
            } else {
                isClickProcessing = false
            }
        } else {
            isClickProcessing = false
        }
    }

    // Метод для перемешивания списка чисел
    private fun shuffle(arr: MutableList<Int>) {
        for (i in arr.size - 1 downTo 1) {
            val j = floor(Random.nextDouble() * (i + 1)).toInt()
            arr.swap(i, j)
        }
    }

    // Метод для начала игры
    private fun startGame(difficulty: Int) {
        pairCount = difficulty
        isGameInProgress = true
        setupContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
        restartButton.visibility = View.GONE
        gameMessage.visibility = View.GONE
        timerDisplay.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        timeInSeconds = 30
        updateTimerDisplay()

        val columns: Int
        val rows: Int

        when (difficulty) {
            4 -> {
                columns = 2
                rows = 4
            }
            6 -> {
                columns = 3
                rows = 4
            }
            8 -> {
                columns = 4
                rows = 4
            }
            10 -> {
                columns = 4
                rows = 5
            }
            else -> {
                columns = 2
                rows = 4
            }
        }

        gameContainer.columnCount = columns
        gameContainer.rowCount = rows

        if (!::timer.isInitialized) {
            timer = object : CountDownTimer((timeInSeconds * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeInSeconds--
                    val timeLeftText = getString(R.string.time_left, timeInSeconds)
                    timerDisplay.text = timeLeftText
                }

                override fun onFinish() {
                    endGame()
                }
            }.start()
        }

        val numbers = createNumbersArray(pairCount).toMutableList()
        shuffle(numbers)

        for (number in numbers) {
            val card = createCard(number)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                leftMargin = marginBetweenCards
                topMargin = marginBetweenCards
                rightMargin = marginBetweenCards
                bottomMargin = marginBetweenCards
            }
            gameContainer.addView(card, params)
        }
    }

    // Метод для возврата в главное меню
    private fun returnToMainMenu() {
        resetGame()
        setupContainer.visibility = View.VISIBLE
        gameContainer.visibility = View.GONE
        restartButton.visibility = View.GONE
        gameMessage.visibility = View.GONE
        timerDisplay.visibility = View.GONE
        backButton.visibility = View.GONE // Скрываем кнопку "Назад" при возвращении в главное меню

//        if (interstitialAd != null) {
//            showInterstitialAd()
//        } else {
//            // Ad is not loaded, or interstitialAd is null
//            // You can handle this case based on your application's logic
//            // For example, you might want to load a new interstitial ad or take another action.
//            loadInterstitialAd()
//        }
    }

    // Метод для отображения поздравления с завершением игры
    private fun displayCongratulations() {
        isDialogVisible = true // Устанавливаем флаг при отображении диалогового окна
        gameMessage.setText(R.string.congratulations_message)
        gameMessage.visibility = View.VISIBLE
    }

    // Метод для скрытия диалогового окна
    private fun hideDialog() {
        isDialogVisible = false // Сбрасываем флаг при скрытии диалогового окна
        gameMessage.visibility = View.GONE
    }

    // Метод для завершения игры
    private fun endGame() {
        isGameInProgress = false
        gameContainer.visibility = View.GONE
        restartButton.visibility = View.VISIBLE
        gameMessage.text = "Время вышло! Попробуйте еще раз."
        gameMessage.visibility = View.VISIBLE
        timerDisplay.visibility = View.GONE
        gameContainer.removeAllViews()
        isClickProcessing = false

        // Скрываем кнопку "Назад" только если диалоговое окно не отображается
        if (!isDialogVisible) {
            backButton.visibility = View.GONE
        }
    }

    // Метод для сброса игры
    private fun resetGame() {
        isGameInProgress = false
        gameContainer.visibility = View.GONE
        restartButton.visibility = View.GONE
        gameMessage.visibility = View.GONE
        setupContainer.visibility = View.VISIBLE
        timerDisplay.visibility = View.GONE
        gameContainer.removeAllViews()
        timer.cancel()
        timeInSeconds = 60
    }

    // Метод для преобразования сложности в количество пар карт
    private fun difficultyToPairCount(difficulty: Int): Int {
        return when (difficulty) {
            0 -> 4
            1 -> 6
            2 -> 8
            3 -> 10
            else -> 4
        }
    }

    // Метод для проверки завершения игры
    private fun checkGameCompletion() {
        if (::timer.isInitialized) {
            val matchedCards = mutableListOf<View>()

            for (i in 0 until gameContainer.childCount) {
                val child = gameContainer.getChildAt(i)
                if (child.tag == "matched") {
                    matchedCards.add(child)
                }
            }

            if (matchedCards.size == pairCount * 2 || timeInSeconds <= 0) {
                timer.cancel()
                endGame() // Вызываем endGame() здесь, чтобы скрыть gameContainer

                if (matchedCards.size == pairCount * 2) {
                    // Если все пары карт собраны, отображаем поздравление
                    displayCongratulations()
                }

                restartButton.visibility = View.VISIBLE
                gameContainer.removeAllViews() // Удаляем все представления (карты) из gameContainer
            }
        }
    }

    // Метод для обмена элементов списка
    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}
