package coval

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import coval.ValidatorCombinators.and
import coval.ValidatorCombinators.or
import coval.ValidatorCombinators.then

data class UserProfile(
    val name: String,
    val email: String,
    val age: Int,
    val address: Address?,
    val preferences: List<String>,
    val isAdmin: Boolean
)

data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

class LargeCombinator: StringSpec({
    "Validate user directly with single validator" {
        validateUser(user) shouldBe user.right()
    }

    "Validate user with multiple validators" {
        val validator = ValidatorClient<String, UserProfile>(listOf(validateName, validateEmail, validateAge, validateStreet, validateCity, validateZipCode))
        validator.validate(user) shouldBe Validated(user, listOf())

        validator.validate(emptyUser) shouldBe Validated(emptyUser, listOf(
            "Name cannot be empty",
            "Invalid email",
            "Age cannot be negative",
            "Street cannot be empty",
            "City has to be Oslo",
            "Zip code has to be a number"
        ))
    }

    /*
     TODO:  Setup Validator for custom pluggable logging solutions
            also make custom Validator types for different combinators that will change the way a log is generated for its errors.
     */

})

val user = UserProfile(
    name = "John Doe",
    email = "test@test.test",
    age = 30,
    address = Address("Test street", "Oslo", "1234"),
    preferences = listOf("Test"),
    isAdmin = true
)

val emptyUser = UserProfile(
    name = "",
    email = "",
    age = -1,
    address = Address("", "", ""),
    preferences = emptyList(),
    isAdmin = false
)

// Some naive non-transforming validation functions
val validateName: Validator<String, UserProfile> = Validator({ profile ->
    if (profile.name.length > 0) profile.right()
    else listOf("Name cannot be empty").left()
})

val validateEmail: Validator<String, UserProfile> = Validator({ profile ->
    if (profile.email.contains("@")) profile.right()
    else listOf("Invalid email").left()
})

val validateAge: Validator<String, UserProfile> = Validator({ profile ->
    if (profile.age >= 0) profile.right()
    else listOf("Age cannot be negative").left()
})

val validateStreet: Validator<String, UserProfile> = Validator({ profile ->
    if ((profile.address?.street?.length ?: 0) > 0) profile.right()
    else listOf("Street cannot be empty").left()
})

val validateOslo: Validator<String, UserProfile> = Validator({ profile ->
    if ((profile.address?.city ?: "") == "Oslo") profile.right()
    else listOf("City has to be Oslo").left()
})

val validateBergen: Validator<String, UserProfile> = Validator({ profile ->
    if ((profile.address?.city ?: "") == "Bergen") profile.right()
    else listOf("City has to be Bergen").left()
})

val validateCity: Validator<String, UserProfile> = (validateOslo or validateBergen)
    .joinErrors { errors -> listOf(errors.joinToString(separator = " or ")) }

val validateIsNumeric: Validator<String, String?> = Validator({ number ->
    if (number?.toIntOrNull() != null) number.right()
    else listOf("Not a number").left()
})

val validateZipCode: Validator<String, UserProfile> = Validator({ profile -> when(val result = validateIsNumeric(profile.address?.zipCode)) {
        is Either.Left -> listOf("Zip code has to be a number").left()
        is Either.Right -> if(result.value?.length == 4) profile.right()
            else listOf("Zip code has to be 4 digits").left()
    }
})

val validateAddress = validateStreet and validateCity and validateZipCode

val isAdmin: Validator<String, UserProfile> = Validator({ profile ->
    if (profile.isAdmin) profile.right()
    else listOf("User is not an admin").left()
})

val validateUser = (
        validateName
        then validateEmail
        then validateAge
        then validateAddress)

val validateAdmin = validateUser then isAdmin


