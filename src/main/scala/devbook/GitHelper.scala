package devbook

import java.io.File

import org.eclipse.jgit.api.Git

trait GitHelper {
  def cloneRepository(uri: String): Either[Git, Error]
  def getRepositories: List[Git]
}

class GitHelperImpl extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  override def cloneRepository(uri: String): Either[Git, Error] =
    try {
      Left(
        Git
          .cloneRepository()
          .setURI(uri)
          .setDirectory(new File(s"$repoFolder/${uriToFolder(uri)}"))
          .call()
      )
    } catch {
      case e: Exception =>
        Right(new Error(e))
    }

  def uriToFolder(uri: String): String = Math.abs(uri.hashCode).toString

  override def getRepositories: List[Git] = {
    val repos = new File(repoFolder).listFiles(_.isDirectory).toList
    repos.map(file => Git.open(file))
  }
}
