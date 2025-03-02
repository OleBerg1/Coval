package coval

import arrow.core.Either
import arrow.core.left
import no.oleberg.Validator.ErrorJoining
import no.oleberg.Validator.Validator
import no.oleberg.Validator.ValidatorFunction

object ValidatorCombinators {
    fun <E, T> ValidatorFunction<E, T>.toValidator(errorJoining: ErrorJoining<E>? = null): Validator<E, T> = Validator(this, errorJoining)


    /*
     * This combinator assumes two validators A and B, where both A and B validates the same input.
     * Right results will be those of the first successful validator.
     * Left results will be the concatenation of all error messages from both validators.
     */
    infix fun <E, T> Validator<E, T>.or(other: Validator<E, T>): Validator<E, T> = Validator({ t ->
        when (val result = this(t)) {
            is Either.Left -> {
                when (val otherResult = other(t)) {
                    is Either.Left -> (result.value + otherResult.value).left()
                    is Either.Right -> otherResult
                }
            }

            is Either.Right -> result
        }
    })

    /*
     * This combinator assumes two validators A and B, where both A and B validates the same input.
     * If a transformation is made, then it will always be the transformation made in B.
     * This is because the transformation in A is assumed to be the identity function.
     * For transformations in A, use the `then` combinator.
     */
    infix fun <E, T> Validator<E, T>.and(other: Validator<E, T>): Validator<E, T> = Validator({ t ->
        when (val result = this(t)) {
            is Either.Left -> result
            is Either.Right -> {
                when (val otherResult = other(t)) {
                    is Either.Left -> result
                    is Either.Right -> otherResult
                }
            }
        }
    })

    /*
     * This combinator assumes two validators A and B, where B operates on some value produced by A.
     */
    infix fun <E, T> Validator<E, T>.then(other: Validator<E, T>): Validator<E, T> = Validator({ t ->
        when (val result = this(t)) {
            is Either.Left -> result
            is Either.Right -> other(result.value)
        }
    })
}