# Code Guidelines
## Kotlin
Please follow the [Kotlin conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).  
If the Code Guidelines and the official conventions clash, use the Kotlin conventions.  
It is highly recommended that you enable the style in your IDE.  

### General advice
When writing code, if shortening the code will make it longer, but making it longer will make it more readable, always go for the longer and more readable code. We don't write code for computers. We write it for humans.

### Specifics
#### Required
- Use `fun foo() = x` instead of `fun foo() { return x }`. Not required, but if possible, omit the return type.
- Put a new line after any control flow statements, unless the control flow statement is a one liner.
- Put a new line before a return, unless the method is a one liner. However, one liner methods are discouraged.
- Put code in a lambda on a new line if it is over 1 line or is long.
- Use `UpperCamelCase` for class names. Use `lowerCamelCase` for method names, variable names, and names of fields that are not
both static and final. Use `UPPER_SNAKE_CASE` for names of fields that are both static and final.
- Method names should generally be verb phrases (`tick`, `getCarversForStep`), except for "withX", "toX", "fromX", "of" and
builder methods.
- Class names and non-boolean field and variable names should be noun phrases (`ChunkRegion`, `color`).
- Boolean field and variable names should always be adjective phrases or present tense verb phrases (`powered`, `canOpen`),
avoiding the `is` and `has` prefixes when possible (`colored`, not `isColored` or `hasColor`).
- To make code as easy to read as possible, keep names in the natural language order.
- Use American English for consistency.
- Omit words that are made redundant by parameter names or owner class names.
- Treat acronyms as single words rather than capitalising every letter. This improves readability (compare `JsonObject` and
`JSONObject`).

#### Recommended
- Omit the types of variables when possible.
- Use `it` in lambda parameters when possible, unless the use of `it` makes the code more confusing.
- Enum constants should be in `THE_NAME` format.
- Prefer method references over lambdas.

#### Do not
- Do not use semicolons to use multiple statements on one line.
- Do not use string concatenation. Instead, use string templates.
- Do not use `\n` in strings. Instead, use multiline strings.
- Do not prefix class names with `I`/`Interface` or `E`/`Enum`.

#### Refrain
- Refrain from trailing commas, except in enums.
- Refrain from using `init` blocks when the variable can be assigned in its declaration.
- Refrain from using `init` blocks or variable declaration + assign when the value is directly from a constructor. Instead, declare it in the constructor.
- Avoid abbreviations unless it's a common one everyone knows.

##### Kotlin Conventions Notes
**This is not an exhaustive list.**  
The people over at FabricMC have created wonderful, more in-depth conventions. [See it here.](https://github.com/FabricMC/yarn/blob/20w46a/CONVENTIONS.md)  
Please note that this is meant for Java code, and not Kotlin code. The Kotlin Conventions overrides these conventions if they clash.
Exceptions to the FabricMC conventions:  
- Refrain from using `@return` or `@param`, as described in the Kotlin Conventions.

### Note
This document was last edited 2020-11-24T01:17:56.366484Z (ISO 8601).  
**Please check regularly to see if there are any changes.**