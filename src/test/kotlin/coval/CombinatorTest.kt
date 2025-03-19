package coval

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import coval.ValidatorCombinators.and
import coval.ValidatorCombinators.or
import coval.ValidatorCombinators.then
import coval.ValidatorCombinators.toValidator

class CombinatorTest: StringSpec({
    "Then should apply the first validator and then the second validator" {
        val person = Person("John", 30)
        val person2 = Person("John", 101)
        val person3 = Person("Jane", 30)

        personValidator(person) shouldBe person.right()
        personValidator(person2) shouldBe listOf("Age must be between 0 and 100").left()
        personValidator(person3) shouldBe listOf("Name must be John").left()
    }

    "If a transformation is made through `then`, then the second validator should use the output of the first" {
        val person = Person("Alice", 30)
        val nameThenName = nameValidator then nameValidator

        nameThenName(person) shouldBe listOf("Name must be John").left()
    }

    "And should apply both validators on the original input" {
        // nameValidator will force transformation on this, transformed output is not valid
        val person = Person("Alice", 30)
        val nameAndName = nameValidator and nameValidator

        nameAndName(person) shouldBe Person("Bob", 30).right()
    }

    "or should apply both validators on the original input" {
        val person = Person("Alice", 30)

        val isJohn = {
            person: Person -> when (person.name) {
                "John" -> person.right()
                else -> listOf("Name must be John").left()
            }
        }.toValidator()

        val isBob = {
            person: Person -> when (person.name) {
                "Bob" -> person.right()
                else -> listOf("Name must be Bob").left()
            }
        }.toValidator()

        val isJohnOrBob = isJohn or isBob
        val nameOrName = nameValidator or nameValidator

        nameOrName(person) shouldBe Person("Bob", 30).right()
        isJohnOrBob(Person("Bob", 30)) shouldBe Person("Bob", 30).right()

        isJohnOrBob(Person("Alice", 30)) shouldBe listOf(
            "Name must be John",
            "Name must be Bob"
        ).left()
    }

    "Or-combinator with a joining function should transform errors correctly" {
        val errorValidator1: Validator<String, Person> = { _: Person -> listOf("Error 1").left() }.toValidator()
        val errorValidator2: Validator<String, Person> = { _: Person -> listOf("Error 2").left() }.toValidator()
        val errorJoiner: ErrorJoining<String> = { errors: List<String> -> listOf(errors.joinToString(separator = " or "))}

        val errorValidator = (errorValidator1 or errorValidator2).joinErrors(errorJoiner)

        errorValidator(Person("John", 30)) shouldBe listOf("Error 1 or Error 2").left()
    }

    "You should be able to map Either<List<String>, T> to Validated<T>" {
        val person = Person("Alice", 30)
        val nameThenName = nameValidator then nameValidator

        val validatedPerson: Validated<Person> = nameThenName(person).toValidated(person)

        validatedPerson shouldBe Validated(Person("Alice", 30), listOf("Name must be John"))
    }

    "You should be able to concatenate two Validated<T> objects of the same value" {
        val validated1 = Validated(1, listOf("Error 1"))
        val validated2 = Validated(1, listOf("Error 2"))
        val validated3 = Validated(2, listOf("Error 3"))

        validated1 + validated2 shouldBe Validated(1, listOf("Error 1", "Error 2"))

        try {
            validated1 + validated3

            throw AssertionError("Should not be able to concatenate two Validated objects with different values")
        } catch (e: IllegalArgumentException) {
            e.message shouldBe "Cannot concatenate two Validated objects with different values"
        }
    }

    "validate should return a Validated object with the correct errors, and any transformation made by the single validator" {
        val person = Person("Alice", 30)
        val person2 = Person("John", 30)
        val person3 = Person("Ada", 30)

        val validator = ValidatorClient(listOf(personValidator))
        val validatedPerson = validator.validate(person)
        val validatedPerson2 = validator.validate(person2)
        val validatedPerson3 = validator.validate(person3)

        validatedPerson shouldBe Validated(Person("Bob", 30), listOf())
        validatedPerson2 shouldBe Validated(Person("John", 30), listOf())
        validatedPerson3 shouldBe Validated(Person("Ada", 30), listOf("Name must be John"))
    }


    "ValidatorClient with several validators should store any successful transformations" {
        val transformingValidator: Validator<String, Person> = { person: Person -> Person("Bob", person.age).right() }.toValidator()
        val failingValidator: Validator<String, Person>  = { _: Person -> listOf("This will always fail").left()}.toValidator()

        val person = Person("Alice", 30)

        val validator = ValidatorClient(listOf(transformingValidator, failingValidator))
        val validator2 = ValidatorClient(transformingValidator, failingValidator)

        val validatedPerson = validator.validate(person)
        val validatedPerson2 = validator2.validate(person)

        validatedPerson shouldBe Validated(Person("Bob", 30), listOf("This will always fail"))
        validatedPerson2 shouldBe Validated(Person("Bob", 30), listOf("This will always fail"))
    }
})

data class Person(val name: String, val age: Int)

val ageValidator: Validator<String, Person> = { person: Person ->
    when(person.age) {
        in 0..100 -> person.right()
        else -> listOf("Age must be between 0 and 100").left()
    }
}.toValidator()

val nameValidator: Validator<String, Person>  = { person: Person ->
    when (person.name) {
        ""          -> Person("John", person.age).right() // Person of no name is named John
        "Alice"     -> Person("Bob", person.age).right() // Alice is renamed to Bob
        "John"      -> person.right()
        else        -> listOf("Name must be John").left()
    }
}.toValidator()

val personValidator = nameValidator then ageValidator
