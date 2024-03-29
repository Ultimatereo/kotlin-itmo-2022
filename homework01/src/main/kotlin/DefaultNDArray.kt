interface NDArray: SizeAware, DimentionAware {
    /*
     * Получаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun at(point: Point): Int

    /*
     * Устанавливаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun set(point: Point, value: Int)

    /*
     * Копируем текущий NDArray
     *
     */
    fun copy(): NDArray

    /*
     * Создаем view для текущего NDArray
     *
     * Ожидается, что будет создан новая реализация интерфейса.
     * Но она не должна быть видна в коде, использующем эту библиотеку как внешний артефакт
     *
     * Должна быть возможность делать view над view.
     *
     * In-place-изменения над view любого порядка видна в оригнале и во всех view
     *
     * Проблемы thread-safety игнорируем
     */
    fun view(): NDArray

    /*
     * In-place сложение
     *
     * Размерность other либо идентична текущей, либо на 1 меньше
     * Если она на 1 меньше, то по всем позициям, кроме "лишней", она должна совпадать
     *
     * Если размерности совпадают, то делаем поэлементное сложение
     *
     * Если размерность other на 1 меньше, то для каждой позиции последней размерности мы
     * делаем поэлементное сложение
     *
     * Например, если размерность this - (10, 3), а размерность other - (10), то мы для три раза прибавим
     * other к каждому срезу последней размерности
     *
     * Аналогично, если размерность this - (10, 3, 5), а размерность other - (10, 3), то мы для пять раз прибавим
     * other к каждому срезу последней размерности
     */
    fun add(other: NDArray)

    /*
     * Умножение матриц. Immutable-операция. Возвращаем NDArray
     *
     * Требования к размерности - как для умножения матриц.
     *
     * this - обязательно двумерна
     *
     * other - может быть двумерной, с подходящей размерностью, равной 1 или просто вектором
     *
     * Возвращаем новую матрицу (NDArray размерности 2)
     *
     */
    fun dot(other: NDArray): NDArray
}

interface NDArrayFactory {
    fun ones(shape: Shape) : NDArray
    fun zeros(shape: Shape) : NDArray
}

/*
 * Базовая реализация NDArray
 *
 * Конструкторы должны быть недоступны клиенту
 *
 * Инициализация - через factory-методы ones(shape: Shape), zeros(shape: Shape) и метод copy
 */
class DefaultNDArray private constructor
    (private val defaultValue: Int,
     private val shape: Shape,
     private val array : MutableMap<Point, Int> = HashMap()): NDArray {


    companion object : NDArrayFactory {
        override fun ones(shape: Shape) = DefaultNDArray(1, shape)
        override fun zeros(shape: Shape) = DefaultNDArray(0, shape)
    }
    private fun checkPoint(point: Point) {
        if (point.ndim != this.ndim) {
            throw NDArrayException.IllegalPointDimensionException()
        }
        for (i in 0 until point.ndim) {
            if (point.dim(i) < 0 || point.dim(i) >= shape.dim(i)) {
                throw NDArrayException.IllegalPointCoordinateException()
            }
        }
    }
    private fun getValue(point: Point) : Int {
        return array.getOrDefault(point, defaultValue)
    }
    override fun at(point: Point): Int {
        checkPoint(point)
        return getValue(point)
    }

    override fun set(point: Point, value: Int) {
        checkPoint(point)
        array[point] = value
    }

    override fun copy(): NDArray {
        return DefaultNDArray(defaultValue, shape, HashMap(array))
    }

    override fun view(): NDArray {
        return DefaultNDArray(defaultValue, shape, array)
    }

    private fun getNextPoint(point: Point, shape: Shape) : DefaultPoint {
        var c = 1
        val newArray = IntArray(shape.ndim)
        for (i in 0 until point.ndim) {
            newArray[i] = (point.dim(i) + c) % shape.dim(i)
            c = (point.dim(i) + c) / shape.dim(i)
        }
        return DefaultPoint(*newArray)
    }

    private fun checkDimensions(other: NDArray, offset : Int) : Boolean {
        for (i in 0 until this.ndim - offset) {
            if (this.dim(i) != other.dim(i)) {
                return false
            }
        }
        return true
    }

    override fun add(other: NDArray) {
        val newArray : MutableMap<Point, Int> = HashMap()
        if (this.ndim < other.ndim) {
            throw NDArrayException.IllegalPointDimensionException()
        }
        val offset = this.ndim - other.ndim
        if (!checkDimensions(other, offset)) {
            throw NDArrayException.IllegalPointDimensionException()
        }
        val startPoint = DefaultPoint(*IntArray(this.ndim){ 0 })
        var point = DefaultPoint(*IntArray(this.ndim){ 0 })
        do {
            newArray[point] = this.at(point) + other.at(reducePoint(point, offset))
            point = getNextPoint(point, this.shape)
        } while (point != startPoint)
        newArray.forEach{ (k, v) -> array.merge(k, v) {_, newval -> newval} }
    }

    private fun reducePoint(defaultPoint: DefaultPoint, offset: Int): DefaultPoint {
        return DefaultPoint(*defaultPoint.getArray().slice(0 until defaultPoint.ndim - offset).toIntArray())
    }

    private fun convertToMatrix(ndArray: NDArray) : Array<IntArray> {
        if (ndArray.ndim == 2) {
            val array = Array(ndArray.dim(0)) { IntArray(ndArray.dim(1)) }
            for (i in 0 until ndArray.dim(0)) {
                for (j in 0 until ndArray.dim(1)) {
                    array[i][j] = ndArray.at(DefaultPoint(i, j))
                }
            }
            return array
        }
        val array = Array(ndArray.dim(0)) {IntArray (1)}
        for (i in 0 until ndArray.dim(0)) {
            array[i][0] = ndArray.at(DefaultPoint(i))
        }
        return array
    }

    private fun convertToNDArray2(array: Array<IntArray>) : NDArray {
        val shape = DefaultShape(array.size, array[0].size)
        val newArray : MutableMap<Point, Int > = HashMap()
        for (i in 0 until shape.dim(0)) {
            for (j in 0 until shape.dim(1)) {
                newArray[DefaultPoint(i, j)] = array[i][j]
            }
        }
        return DefaultNDArray(0, shape, newArray)
    }
    private fun convertToNDArray1(array: Array<IntArray>) : NDArray {
        val shape = DefaultShape(array.size)
        val newArray : MutableMap<Point, Int > = HashMap()
        for (i in 0 until shape.dim(0)) {
            newArray[DefaultPoint(i)] = array[i][0]
        }
        return DefaultNDArray(0, shape, newArray)
    }

    override fun dot(other: NDArray): NDArray {
        if (this.ndim != 2 || other.ndim > 2) {
            throw NDArrayException.IllegalPointDimensionException()
        }
        if (other.ndim == 1 && other.dim(0) == 1) {
            val newArray : MutableMap<Point, Int> = HashMap()
            val startPoint = DefaultPoint(*IntArray(2) { 0 })
            var point = DefaultPoint(*IntArray(2) { 0 })
            do {
                newArray[point] = this.at(point) * other.at(startPoint)
                point = getNextPoint(point, this.shape)
            } while (point != startPoint)
            return DefaultNDArray(0, this.shape, newArray)
        }
        val matrix1 = convertToMatrix(this)
        val matrix2 = convertToMatrix(other)
        if (matrix1[0].size != matrix2.size) {
            throw NDArrayException.IllegalPointDimensionException()
        }
        val matrix3 = Array(matrix1.size) { IntArray(matrix2[0].size) }
        for (i in matrix1.indices) {
            for (j in 0 until matrix1[0].size) {
                for (k in 0 until matrix2[0].size) {
                    matrix3[i][k] += matrix1[i][j]*matrix2[j][k]
                }
            }
        }
        if (other.ndim == 2) {
            return convertToNDArray2(matrix3)
        }
        return convertToNDArray1(matrix3)
    }
    override val size: Int
        get() = shape.size
    override val ndim: Int
        get() = shape.ndim

    override fun dim(i: Int): Int = shape.dim(i)
}


sealed class NDArrayException : Exception() {
    class IllegalPointCoordinateException : NDArrayException()
    class IllegalPointDimensionException : NDArrayException()
}