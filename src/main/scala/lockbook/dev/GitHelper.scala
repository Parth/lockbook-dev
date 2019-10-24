package lockbook.dev

import java.io.File
import java.net.URI

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

trait GitHelper {
  def cloneRepository(uri: String): Either[GitError, Git]
  def getRepositories: List[Git]
  def getRepoName(git: Git): String
  def commit(message: String, git: Git): Either[GitError, Unit]
  def push(git: Git): Either[GitError, Unit]
  def commitAndPush(message: String, git: Git): Either[GitError, Unit]
  def pull(git: Git): Either[GitError, Unit]
  def sync(git: Git): Either[GitError, Unit]
  def deleteRepo(git: Git): Either[FileError, Unit]
  def pullNeeded(git: Git): Either[GitError, Boolean]
  def localDirty(git: Git): Boolean
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper, fileHelper: FileHelper) extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  override def cloneRepository(uri: String): Either[GitError, Git] = {
    getCredentials(new URI(uri)).flatMap(credentials => {
      try {
        Right(
          Git
            .cloneRepository()
            .setURI(uri)
            .setDirectory(new File(s"$repoFolder/${uriToFolder(uri)}"))
            .setCredentialsProvider(credentials)
            .call()
        )
      } catch {
        case te: TransportException =>
          gitCredentialHelper.deleteStoredCredentials(new URI(uri).getHost)
          Left(InvalidCredentialsOrNetwork(te))
        case e: Exception =>
          Left(UnknownException(e))
      }
    })
  }

  override def commit(message: String, git: Git): Either[GitError, Unit] = {
    try {
      git.add().addFilepattern(".").call()
      git.commit().setMessage(message).call()
      Right(())
    } catch {
      case e: Exception =>
        Left(UnknownException(e))
    }
  }

  override def push(git: Git): Either[GitError, Unit] = {
    getCredentials(git)
      .flatMap(credentials => {
        try {
          git
            .push()
            .setCredentialsProvider(credentials)
            .call()

          Right(())
        } catch {
          case te: TransportException =>
            Left(InvalidCredentialsOrNetwork(te))
          case e: Exception =>
            Left(UnknownException(e))
        }
      })
  }

  override def commitAndPush(message: String, git: Git): Either[GitError, Unit] = {
    commit(message, git)
      .flatMap(_ => push(git))
  }

  override def sync(git: Git): Either[GitError, Unit] = {
    commit("", git)
      .flatMap(_ => pull(git))
      .flatMap(_ => push(git))
  }

  override def getRepositories: List[Git] = {
    val repos = new File(repoFolder)
    if (repos.exists()) {
      repos
        .listFiles(_.isDirectory)
        .toList
        .map(file => Git.open(file))
    } else {
      List()
    }
  }

  override def deleteRepo(git: Git): Either[FileError, Unit] = {
    val file = git.getRepository.getWorkTree
    fileHelper.recursiveFileDelete(file)
  }

  private def getCredentials(git: Git): Either[UserCanceled, UsernamePasswordCredentialsProvider] =
    getCredentials(getRepoURI(git))

  private def getCredentials(
      uri: URI
  ): Either[UserCanceled, UsernamePasswordCredentialsProvider] = {
    gitCredentialHelper
      .getCredentials(uri.getHost)
      .map(c => new UsernamePasswordCredentialsProvider(c.username, c.password))
  }

  def uriToFolder(uri: String): String = {
    val folderName = uri.split("/").last
    if (folderName.endsWith(".git")) {
      folderName.substring(0, folderName.length - 4)
    } else {
      folderName
    }
  }

  def getRepoName(git: Git): String = {
    val repoUrl = git.getRepository.getConfig
      .getString("remote", "origin", "url")

    uriToFolder(repoUrl)
  }

  def getRepoURI(git: Git): URI = {
    val repoUrl = git.getRepository.getConfig
      .getString("remote", "origin", "url")

    new URI(repoUrl)
  }

  override def pull(git: Git): Either[GitError, Unit] = {
    getCredentials(git).flatMap(credentials => {
      try {
        Right(
          git
            .pull()
            .setCredentialsProvider(credentials)
            .call()
        )
      } catch {
        case te: TransportException =>
          Left(InvalidCredentialsOrNetwork(te))
        case e: Exception =>
          Left(UnknownException(e))
      }
    })
  }

  override def pullNeeded(git: Git): Either[GitError, Boolean] = {
    getCredentials(git)
      .flatMap { credentials =>
        try {
          val doRefsMatch = git
            .fetch()
            .setCredentialsProvider(credentials)
            .call()
            .getAdvertisedRef("HEAD")
            .getObjectId
            .getName
            .equals(git.getRepository.getAllRefs.get("HEAD").getObjectId.getName)

          Right(!doRefsMatch)
        } catch {
          case te: TransportException =>
            Left(InvalidCredentialsOrNetwork(te))
          case e: Exception =>
            Left(UnknownException(e))
        }
      }
  }

  override def localDirty(git: Git): Boolean = !git.status().call().isClean // TODO Handle failure?
}
