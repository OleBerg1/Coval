package coval

import arrow.core.Either

data class Validated<E, T>(
    val value: T,
    val errors: List<E>
){
    fun throwIfFailed(throwable: (List<E>) -> Throwable): Validated<E, T> =
        if (errors.isEmpty()) this
        else throw throwable(errors)

    infix operator fun plus(other: Validated<E, T>): Validated<E, T> {
        if (this.value != other.value) {
            throw IllegalArgumentException("Cannot concatenate two Validated objects with different values")
        }

        return Validated(this.value, this.errors + other.errors)
    }
}

fun<E, T> Either<List<E>, T>.toValidated(value: T): Validated<E, T> {
    return when (this) {
        is Either.Left -> Validated(value, this.value)
        is Either.Right -> Validated(this.value, emptyList())
    }
}
