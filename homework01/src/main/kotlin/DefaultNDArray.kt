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
        return this
    }

    override fun add(other: NDArray) {
        TODO("Not yet implemented")
    }

    override fun dot(other: NDArray): NDArray {
        TODO("Not yet implemented")
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