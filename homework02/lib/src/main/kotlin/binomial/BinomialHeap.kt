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
    private tailrec fun plusImpl1(first:FList<BinomialTree<T>>, second: FList<BinomialTree<T>>,
                         result: FList<BinomialTree<T>> = FList.nil()) : FList<BinomialTree<T>> {
        if (first.isEmpty && second.isEmpty) {
            return result.reverse()
        }
        val first1 : FList<BinomialTree<T>>
        val second1 : FList<BinomialTree<T>>
        val result1 : FList<BinomialTree<T>>
        if (first.isEmpty) {
            first1 = first
            second1 = (second as FList.Cons).tail
            result1 = FList.Cons(second.head, result)
        } else if (second.isEmpty) {
            first1 = (first as FList.Cons).tail
            second1 = second
            result1 = FList.Cons(first.head, result)
        } else if ((first as FList.Cons).head.order <
            (second as FList.Cons).head.order) {
            first1 = first.tail
            second1 = second
            result1 = FList.Cons(first.head, result)
        } else {
            first1 = first
            second1 = second.tail
            result1 = FList.Cons(second.head, result)
        }
        return plusImpl1(
            first1,
            second1,
            result1
        )
    }
    private fun getTopOrder(trees: FList<BinomialTree<T>>) : Int {
        if (trees.isEmpty) {
            return -1
        }
        return (trees as FList.Cons).head.order
    }
    private tailrec fun plusImpl2(heap: FList<BinomialTree<T>>,
                          result: FList<BinomialTree<T>> = FList.nil()) : FList<BinomialTree<T>> {
        if (heap is FList.Nil) {
            return result.reverse()
        }
        heap as FList.Cons
        val heapOrder1 = getTopOrder(heap)
        val heapOrder2 = getTopOrder(heap.tail)
        val resultOrder = getTopOrder(result)

        val heap1 : FList<BinomialTree<T>>
        val result1 : FList<BinomialTree<T>>


        if (heap.tail is FList.Cons && heapOrder1 == heapOrder2) {
            heap1 = heap.tail.tail
            result1 =  FList.Cons(heap.head + heap.tail.head, result)
        } else if (result is FList.Cons && heapOrder1 == resultOrder) {
            heap1 = heap.tail
            result1 = FList.Cons(heap.head + result.head, result.tail)
        } else if (heap.tail is FList.Cons && result is FList.Cons && heapOrder2 == resultOrder) {
            heap1 = FList.Cons(heap.head, heap.tail.tail)
            result1 = FList.Cons(heap.tail.head + result.head, result.tail)
        } else {
            heap1 = heap.tail
            result1 = FList.Cons(heap.head, result)
        }
        return plusImpl2(heap1, result1)
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
    private tailrec fun dropImpl(
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

