package devbook

object Errors {
  val noCredentialsProvided = new Error("No Credentials Provided")
  val invalidPath = new Error("This is not a valid path")
  val fileMissing = new Error("File does not exist")
}
