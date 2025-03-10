package no.oleberg.Validator

import coval.Validated
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import java.util.logging.Logger

typealias ValidatorFunction<E, T> = (T) -> Either<List<E>, T>
typealias ErrorJoining<E> = ((List<E>) -> List<E>)

class Validator<E, T>(
    private val validator: ValidatorFunction<E, T>,
    private val errorJoining: ErrorJoining<E>? = null
 ): ValidatorFunction<E, T> {
    override fun invoke(p1: T): Either<List<E>, T> {
        return when (val result = validator(p1)) {
            is Left     -> result.joinErrorsIfPossible()
            is Right    -> result
        }
    }

    private fun Left<List<E>>.joinErrorsIfPossible(): Either<List<E>, T> {
        return errorJoining?.invoke(this.value)?.left() ?: this
    }

    fun joinErrors(errorJoining: ErrorJoining<E>): Validator<E, T> = Validator(this, errorJoining)
}

class ValidatorClient<E, T>(
    // TODO: remove client, not needed anymore
    private val validators: List<Validator<E, T>>,
) {
    /*
        * This function will validate a value of type T using the validators provided in the constructor.
        * It will return a Validated object with the value and a list of error messages.
        *
        * If there is no error, then transformations made by a validator will be kept.
        * If there is an error, then transformations made by that validator will be discarded.
    */
    // TODO: Refactor accumulated errors to a map of Validators to Errors -> This can actually be done for the Eithers
    //  returned by Validator instead of the Validated.
    fun validate(value: T): Validated<T> =
        validators.fold(Validated(value, emptyList())) { acc, validator ->
            val result = validator(acc.value)
            when(result) {
                is Left -> Validated(acc.value, acc.errors + result.value.map { "$it" })
                is Right -> Validated(result.value, acc.errors)
            }
        }
}
