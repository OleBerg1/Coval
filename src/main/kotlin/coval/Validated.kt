package coval

import arrow.core.Either

data class Validated<T>(
    val value: T,
    val errors: List<String>
){
    fun throwIfFailed(throwable: (List<String>) -> Throwable): Validated<T> =
        if (errors.isEmpty()) this
        else throw throwable(errors)

    infix operator fun plus(other: Validated<T>): Validated<T> {
        if (this.value != other.value) {
            throw IllegalArgumentException("Cannot concatenate two Validated objects with different values")
        }

        return Validated(this.value, this.errors + other.errors)
    }
}

fun<E, T> Either<List<E>, T>.toValidated(value: T, transform: (E) -> String = ::defaultTransform): Validated<T> {
    return when (this) {
        is Either.Left -> Validated(value, this.value.map(transform))
        is Either.Right -> Validated(this.value, emptyList())
    }
}

private fun<E> defaultTransform(e: E): String =  "$e"