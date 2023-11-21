package com.example.mycustomview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

typealias OnCellActionListener = (row: Int, column: Int, field: TicTacToeField) -> Unit

class TicTacToeView(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int, //атрибут стандартного стиля глобального
    defStyleRes: Int //стиль по умолчанию
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    var ticTacToeField: TicTacToeField? = null //если будет null, то рисовать нашу View не будем
        set(value) { //сеттер добавили, чтобы если изменилось значение field, то мы перерисовали нашу View
            field?.listeners?.remove(listener) //в field хранится старое поле. При смене поля, мы должны отписаться от слушанья изменений старого поля
            field = value
            field?.listeners?.add(listener)
            updateViewSizes()
            requestLayout() // это нужно вызывать, если изменения наших данных может спровоцировать и изменения размера нашего компонента
            //если наша view имело свойства wrap_content, наше поле было 3х3, а потом решили изменить и сделать 5х5, поле увелчится в размерах. Для этого и вызываем
            //requestLayout()
            invalidate() // метод запускает перерисовку нашего компонента
        }

    var actionListener: OnCellActionListener? = null

    //если прочитать до того, как было присвоиено значение, то выбросит исключение
    //для не примитивных типов мы бы написали просто private lateinit var, а для примитивных использьуется такая конструкция:
    private var player1Color by Delegates.notNull<Int>()
    private var player2Color by Delegates.notNull<Int>()
    private var gridColor by Delegates.notNull<Int>()

    private val fieldRect = RectF(0f, 0f, 0f, 0f)
    private var cellSize = 0f
    private var cellPadding = 0f

    private val cellRect = RectF(0f, 0f, 0f, 0f)

    private lateinit var player1Paint: Paint
    private lateinit var player2Paint: Paint
    private lateinit var currentCellPaint: Paint
    private lateinit var gridPaint: Paint

    private var currentRow = -1
    private var currentColumn = -1

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(
        context,
        attributeSet,
        R.attr.ticTacToeFieldStyle
    )

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attributeSet,
        defStyleAttr,
        R.style.DefaultTicTacToeFieldStyle
    )


    init {
        if (attributeSet != null) {
            initAttributes(attributeSet, defStyleAttr, defStyleRes)
        } else {
            initDefaultColors()
        }
        initPaints()
        //это мод, когда мы находимя внутри андроид студии. Что-то вроде дебага, чтобы во время работы можно было задать значения для ticTacToeField
        if (isInEditMode) {
            ticTacToeField = TicTacToeField(8, 6)
            ticTacToeField?.setSell(4, 2, Cell.PLAYER_1)
            ticTacToeField?.setSell(4, 3, Cell.PLAYER_2)
        }
        isFocusable = true //наша view может находиться в фокусе
        isClickable = true //на нашу View можно нажимать

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false //выключаем анимацию с автоматическим подсвчечиванием, которая появилась после 8 андроида
        }
    }

    private fun initPaints() {
        //Paint.ANTI_ALIAS_FLAG - не будет кривых пикселей, будут полее плавные линии
        player1Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        player1Paint.color = player1Color
        player1Paint.style = Paint.Style.STROKE
        player1Paint.strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)

        player2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        player2Paint.color = player2Color
        player2Paint.style = Paint.Style.STROKE
        player2Paint.strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)

        currentCellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        currentCellPaint.color = Color.rgb(230, 230, 230)
        currentCellPaint.style = Paint.Style.FILL

    }

    //если ничего не было передано, то инициализируем значениями по умолчанию(только в случае, если не было стандартного стиля, глобальной темы приложения стиля,
    //не было передано в XML атрибутах
    private fun initDefaultColors() {
        player1Color = PLAYER1_DEFAULT_COLOR
        player2Color = PLAYER2_DEFAULT_COLOR
        gridColor = GRID_DEFAULT_COLOR
    }

    //Если были переданы значения для атрибутов, которые мы добавили для нашей View в файле attrs defStylable, то мы должны их распарсить
    private fun initAttributes(attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        //за счет того, что мы передали внутрь и defStyleAttr и defStyleRes, то андроид за нас сам поймет, откуда брать цвета, из разметки, themes из какого именно стиля
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.TicTacToeView,
            defStyleAttr,
            defStyleRes
        )
        player1Color = typedArray.getColor(
            R.styleable.TicTacToeView_player1Color,
            PLAYER1_DEFAULT_COLOR
        ) //Color.GREEN на всякий случай, если вдруг не будет стилей
        player2Color =
            typedArray.getColor(R.styleable.TicTacToeView_player2Color, PLAYER2_DEFAULT_COLOR)
        gridColor = typedArray.getColor(R.styleable.TicTacToeView_gridColor, GRID_DEFAULT_COLOR)

        //освобождаем typedArray
        typedArray.recycle()
    }

    //наша view присоединена куда-то там и можем взаимодействовать с иерархией компонентов
    //в ней мы добавим слушатель на изменениче значение поля, т.е. когда наша view присоединена мы должны слушать изменения ticTacToeField
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticTacToeField?.listeners?.add(listener)
    }

    //вызывается, когда наша View была отсоединена или удалена с активити, фрагмента и т.д. А раз была удалена, то мы не должны уже слушать
    //изменения в tiaTacToeField
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ticTacToeField?.listeners?.remove(listener)
    }

    //создаем сам слушатель изменения. Т.е. когда изменилось поле, то
    private val listener: OnFieldChangedListener = {
        invalidate()
    }

    //компоновщик - как я понял, это просто какой-то ViewGroup, в котором мы находимся(Constaint, Frame)

    //здесь мы можем договориться с компоновщиком, в котором находимся по поводу размера нашей View
    //вызывается, когда компоновщик хочет измеринть размеры нашей View.
    //т.е. смысл этого метода в том, чтобы наш компоновщик и view договорились о ширине и высоте.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //для начала определяем минимальную высоту и ширину нашего компонента. suggested - это на основе minHeight и minWidth из XML или по размеру backgroud
        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        //внутри компонента мы везде работаем с пикселями, а не с dp
        val desiredCellSizeInPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, DESIRED_CELL_SIZE,
            resources.displayMetrics
        ).toInt()

        val rows = ticTacToeField?.rows ?: 0
        val columns = ticTacToeField?.columns ?: 0

        //та ширина, которую мы хотим получить, чтобы компоновщик нам ее назначил
        val desiredWidth =
            max(minWidth, columns * desiredCellSizeInPixels + paddingLeft + paddingRight)
        val desiredHeight =
            max(minHeight, rows * desiredCellSizeInPixels + paddingTop + paddingBottom)

        //делаем ответное предложение компоновщику
        setMeasuredDimension(
            //нужно еще учесть ограничения от нашего компоновщика. Насамом деле в переменные widthMeasureSpec: Int, heightMeasureSpec: Int,
            //хоть они и INT в них сохранены два значения. Первое - ширина, которую нам хотят назначить и плюс ограничители от компоновщика
            //ограничители могут быть:
            // atMost - размер может быть любым, не не больше того размера, что назначил нам компоновщик
            // exactly - только тот размер, который передал компоновщик и никакой другой
            // unspecified - ограничений нет(компоновщик говорит, что если тебе не нравится та ширина, которую я тебе
            //назначил, то можешь исползовать свою. Никаких ограничений нет. resolveSize вычисляет и делать все это за нас
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    //вызывается тогда, когда компоновщик назначил конкретный размер нашему компоненту. Т.е. все, задан конкретный размер нашей View и теперь по этим
    //размерам мы можем определить безопасную зону, в которой можем рисовать
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSizes()
    }

    private fun updateViewSizes() {
        val field = this.ticTacToeField ?: return

        val safeWidth =
            width - paddingLeft - paddingRight //это все пространство, которое для нас доступно с учетом padding
        val safeHeight = height - paddingTop - paddingBottom

        val cellWidth = safeWidth / field.columns.toFloat()
        val cellHeight = safeHeight / field.rows.toFloat()

        cellSize = min(cellWidth, cellHeight)
        cellPadding = cellSize * 0.2f

        val fieldWidth =
            cellSize * field.columns //это реальная ширина и высота поля, которую будем использовать
        val fieldHeight = cellSize * field.rows

        fieldRect.left =
            paddingLeft + (safeWidth - fieldWidth) / 2 //делим на 2, чтобы в safeWidht- это реальный размер доступный нам зоны для рисования наша
        //view была центрирована внутри нее
        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2
        fieldRect.right = fieldRect.left + fieldWidth
        fieldRect.bottom = fieldRect.top + fieldHeight
    }


    //метод, который вызывается, чтобы отрисовать нашу View. Он должен быть максимально оптимизирован. В нем не рекомендуется создавать новые объекты в принципе
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (ticTacToeField == null) return
        if (cellSize == 0f) return
        if (fieldRect.width() <= 0 || fieldRect.height() <= 0) return

        drawGrid(canvas)
        drawCurrentCell(canvas)
        drawCells(canvas)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when(keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> moveCurrentCell(1, 0)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveCurrentCell(0, -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveCurrentCell(0, 1)
            KeyEvent.KEYCODE_DPAD_UP -> moveCurrentCell(-1, 0)
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun moveCurrentCell(rowDiff: Int, columnDiff: Int): Boolean {
        val field = this.ticTacToeField ?: return false
        if(currentRow == -1 || currentColumn == -1) {
            currentRow = 0
            currentColumn = 0
            return true
        } else {
            if(currentColumn + columnDiff < 0) return false
            if(currentColumn + columnDiff >= field.columns) return false
            if(currentRow + columnDiff < 0) return false
            if(currentRow + columnDiff >= field.rows) return false

            currentColumn += columnDiff
            currentRow += rowDiff
            invalidate()
            return true
        }
    }

    private fun drawCurrentCell(canvas: Canvas) {
        if (currentRow == -1 || currentColumn == -1) return
        val cell = getCellRect(currentRow, currentColumn)
        canvas.drawRect(
            cell.left - cellPadding,
            cell.top - cellPadding,
            cell.right + cellPadding,
            cell.bottom + cellPadding, currentCellPaint
        )

    }

    private fun drawGrid(canvas: Canvas) {
        val field = this.ticTacToeField ?: return
        val xStart = fieldRect.left
        val xEnd = fieldRect.right
        for (i in 0..field.rows) {
            val y = fieldRect.top + cellSize * i
            canvas.drawLine(xStart, y, xEnd, y, gridPaint)
        }

        val yStart = fieldRect.top
        val yEnd = fieldRect.bottom
        for (i in 0..field.columns) {
            val x = fieldRect.left + cellSize * i
            canvas.drawLine(x, yStart, x, yEnd, gridPaint)
        }
    }

    private fun drawCells(canvas: Canvas) {
        val field = this.ticTacToeField ?: return

        for (row in 0 until field.rows) {
            for (column in 0 until field.columns) {
                val cell = field.getCell(row, column)
                when (cell) {
                    Cell.PLAYER_1 -> {
                        drawPlayer1(canvas, row, column)
                    }

                    Cell.PLAYER_2 -> {
                        drawPlayer2(canvas, row, column)
                    }

                    Cell.EMPTY -> {

                    }
                }
            }
        }
    }

    private fun drawPlayer1(canvas: Canvas, row: Int, column: Int) {
        val cellRect = getCellRect(row, column)
        canvas.drawLine(cellRect.left, cellRect.top, cellRect.right, cellRect.bottom, player1Paint)
        canvas.drawLine(cellRect.right, cellRect.top, cellRect.left, cellRect.bottom, player1Paint)
    }

    private fun drawPlayer2(canvas: Canvas, row: Int, column: Int) {
        val cellRect = getCellRect(row, column)
        canvas.drawCircle(
            cellRect.centerX(),
            cellRect.centerY(),
            cellRect.width() / 2f,
            player2Paint
        )
    }

    private fun getCellRect(row: Int, column: Int): RectF {
        //работать с одной переменной для всех клеток можно, так как отрисовка выполняется
        //в одном потоке,а также мы не создаем никаких дополнительных объектов в методе onDraw
        cellRect.left = fieldRect.left + cellSize * column + cellPadding
        cellRect.top = fieldRect.top + row * cellSize + cellPadding
        cellRect.right = cellRect.left + cellSize - cellPadding * 2
        cellRect.bottom = cellRect.top + cellSize - cellPadding * 2
        return cellRect
    }

    //работа с событиями нашей view(нажатие). Можно было бы использовать GestureDetector, но у нас просто клики, поэтому нам достаточно простого onTouchEvent
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { //это самое начальное событие и от результата обработки которого будет зависеть, будет ли к нам приходить следующее событие
                updateCurrentCell(event)
                return true //мы это событие приняли, таким образом придут остальные события, которые будут после этого события
            }
            MotionEvent.ACTION_MOVE -> {
                updateCurrentCell(event)
            }
            MotionEvent.ACTION_UP -> { //отпускание пальца
                return performClick()
            }
        }
        return false //если можем обработать событие, возвращаем true, если не можем - false
    }

    override fun performClick(): Boolean {
        super.performClick() // нужно, так как есть некоторая своя реализация
        val field = this.ticTacToeField ?: return false
        val row = currentRow
        val column = currentColumn
        if (row >= 0 && column >= 0 && row < field.rows && column < field.columns) {
            actionListener?.invoke(row, column, field)
            return true
        }
        return false
    }

    private fun updateCurrentCell(event: MotionEvent) {
        val field = this.ticTacToeField ?: return
        val row = getRow(event)
        val column = getColumn(event)
        if (row >= 0 && column >= 0 && row < field.rows && column < field.columns) {
            if(currentRow != row || currentColumn != column) {
                currentRow = row
                currentColumn = column
                invalidate()
            }
        }
    }

    private fun getRow(event: MotionEvent): Int {
        return ((event.y - fieldRect.top) / cellSize).toInt()
    }

    private fun getColumn(event: MotionEvent): Int {
        return ((event.x - fieldRect.left) / cellSize).toInt()
    }


    companion object {
        const val PLAYER1_DEFAULT_COLOR = Color.GREEN
        const val PLAYER2_DEFAULT_COLOR = Color.RED
        const val GRID_DEFAULT_COLOR = Color.GRAY

        const val DESIRED_CELL_SIZE = 50f
    }
}