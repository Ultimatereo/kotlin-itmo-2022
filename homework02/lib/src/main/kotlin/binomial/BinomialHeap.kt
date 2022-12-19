package binomial

import java.lang.IllegalArgumentException
import kotlin.math.min

/*
 * BinomialHeap - реализация биномиальной кучи
 *
 * https://en.wikipedia.org/wiki/Binomial_heap
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 * Детали внутренней реализации должны быть спрятаны
 * Создание - только через single() и plus()
 *
 * Куча совсем без элементов не предусмотрена
 *
 * Операции
 *
 * plus с кучей
 * plus с элементом
 * top - взятие минимального элемента
 * drop - удаление минимального элемента
 *
 * Инвариант
 * Кучу будем хранить в порядке возрастания степеней. В head находится минимальная степень, а в самом конце максимальная
 *
 */
class BinomialHeap<T: Comparable<T>> private constructor(val trees: FList<BinomialTree<T>>): SelfMergeable<BinomialHeap<T>> {
    companion object {
        fun <T: Comparable<T>> single(value: T): BinomialHeap<T> =
            BinomialHeap(FList.Cons(BinomialTree.single(value), FList.Nil()))
    }

    /*
     * слияние куч
     *
     * Требуемая сложность - O(log(n))
     */
    private fun plusImpl(first:FList<BinomialTree<T>>, second: FList<BinomialTree<T>>) : FList<BinomialTree<T>> {
        val tree = plusImpl1(first, second)
        return plusImpl2(tree)
    }
    private fun plusImpl1(first:FList<BinomialTree<T>>, second: FList<BinomialTree<T>>,
                         result: FList<BinomialTree<T>> = FList.nil()) : FList<BinomialTree<T>> {
        if (first.isEmpty && second.isEmpty) {
            return result.reverse()
        }
        if (first.isEmpty) {
            return plusImpl1(
                first,
                (second as FList.Cons).tail,
                FList.Cons(second.head, result))
        }
        if (second.isEmpty) {
            return plusImpl1(
                (first as FList.Cons).tail,
                second,
                FList.Cons(first.head, result))
        }
        if ((first as FList.Cons).head.order <
            (second as FList.Cons).head.order) {
            return plusImpl1(
                first.tail,
                second,
                FList.Cons(first.head, result)
            )
        }
        return plusImpl1(
            first,
            second.tail,
            FList.Cons(second.head, result)
        )
    }
    private fun plusImpl2(heap: FList<BinomialTree<T>>,
                          result: FList<BinomialTree<T>> = FList.nil()) : FList<BinomialTree<T>> {
        if (heap is FList.Nil) {
            return result.reverse()
        }
        if (heap is FList.Cons && heap.tail is FList.Nil) {
            if (result is FList.Cons && heap.head.order == result.head.order) {
                plusImpl2(heap.tail, FList.Cons(heap.head + result.head, result.tail))
            }
            return plusImpl2(heap.tail, FList.Cons(heap.head, result))
        }
        if (heap is FList.Cons && heap.tail is FList.Cons) {
            if (result is FList.Cons) {
                if (heap.head.order == heap.tail.head.order) {
                    return plusImpl2(heap.tail.tail, FList.Cons(heap.head + heap.tail.head, result))
                }
                if (heap.head.order == result.head.order) {
                    return plusImpl2(heap.tail, FList.Cons(heap.head + result.head, result.tail))
                }
                if (heap.tail.head.order == result.head.order) {
                    return plusImpl2(FList.Cons(heap.head, heap.tail.tail), FList.Cons(heap.tail.head + result.head, result.tail))
                }
            }
            if (heap.head.order == heap.tail.head.order) {
                return plusImpl2(heap.tail.tail, FList.Cons(heap.head + heap.tail.head, result))
            }
            return plusImpl2(heap.tail, FList.Cons(heap.head, result))
        }
        throw IllegalArgumentException("It's impossible to reach this part of argument if arguments are correct")
    }

    override fun plus(other :BinomialHeap<T>): BinomialHeap<T> {
        return BinomialHeap(plusImpl(this.trees, other.trees))
    }

    /*
     * добавление элемента
     * 
     * Требуемая сложность - O(log(n))
     */
    operator fun plus(elem: T): BinomialHeap<T> {
        return this + BinomialHeap(flistOf(BinomialTree.single(elem)))
    }

    /*
     * минимальный элемент
     *
     * Требуемая сложность - O(log(n))
     */
    fun top(): T {
        return this.trees.fold((this.trees as FList.Cons<BinomialTree<T>>).head.value)
        { acc, current ->
            if (acc < current.value) {
                acc
            } else {
                current.value
            }
        }
    }

    /*
     * удаление элемента
     *
     * Требуемая сложность - O(log(n))
     */
    private fun dropImpl(
        first: FList<BinomialTree<T>>,
        second: FList<BinomialTree<T>>,
        minValue : T) : FList<BinomialTree<T>> {
        if ((second as FList.Cons).head.value === minValue) {
            val firstTree = first.reverse().add((second as FList.Cons).tail)
            val secondTree = second.head.children
            return plusImpl(firstTree, secondTree)
        }
        return dropImpl(
            FList.Cons((second as FList.Cons).head, first),
            second.tail,
            minValue
        )
    }
    fun drop(): BinomialHeap<T> {
        return BinomialHeap(dropImpl(FList.nil(), this.trees, this.top()))
    }
}

