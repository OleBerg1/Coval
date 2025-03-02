# Coval
#### Composable validation for Kotlin 

Coval is a library that provides a simple and composable way to validate data in Kotlin.

It uses functions as an alias for validation rules, and allow you to combine them, creating complex validation rules.
It also provides tooling for how to handle any error produced by a validation, and makes combined validations easy to read and understand.

Coval relies on Arrow.Either for its flow control.

Example:
```kotlin
data class Person(val name: String, val age: Int)
val person = Person("John", 30)

val validateName = { p: Person -> if (p.name.isNotEmpty()) p.right() else listOf("Name is empty").left() }.toValidator()
val validateAge = { p: Person -> if (p.age > 0) p.right() else listOf("Age is invalid").left() }.toValidator()

val validatePerson = validateName then validateAge // If person has no name, age is not validated
val validateAgeOrName = validateName or validateAge // If person has no name, age is validated
```

Since any resulting validator from a combination will just be a new validator, this concept can go on for ever.

A Validator can also force a transformation on its output value, to handle anticipated errors or to transform the output value.

```kotlin
val john = Person("John", 30)
val validateName = { p: Person ->
    when(Person.name) {
        "John" -> p.copy(name = "Jon").right()
        "Jon" -> listOf("Name can't be Jon").left()
        else -> listOf("Name is invalid").left()
    }
}.toValidator()

// Here the result of the first validation is looked at by the second validation, which fails.
val validatePerson = validateName then validateName 

// Both validations will run on the original input, and neither will fail. 
val validatePerson = validateName and validateAge 
```

Each Combinator will return a new Validator, retaining the original validators for reuse.
A combinator has built in structure for joining errors together.
This is a function of type `(List<E>) -> List<E>` which can be used to join errors together in a more readable way.
for certain combinators you may want to join error messages, or even replacement values in a custom way. 

Meaning if you have a Validator of type `Validator<A, A>`, you can set up failed validations to return replacement values.


```kotlin
// Will return "Name is empty and Age is invalid" when ran.
// - the joining function is enclosed  within the validators, and will be called when the validator is run.
val validatePerson = (validateName or validateAge)
    .joinErrors { errors: List<String> -> listOf(errors.joinToString = " and ") }
```