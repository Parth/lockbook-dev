package lockbook.dev

import java.io.File
import java.net.URI

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

trait GitHelper {
  def cloneRepository(uri: String): Either[LockbookError, Git]
  def getRepositories: List[Git]
  def getRepoName(git: Git): String
  def commit(message: String, git: Git): Either[LockbookError, Unit]
  def push(git: Git): Either[LockbookError, Unit]
  def commitAndPush(message: String, git: Git): Either[LockbookError, Unit]
  def pull(git: Git): Either[LockbookError, Unit]
  def sync(git: Git): Either[LockbookError, Unit]
  def deleteRepo(git: Git): Either[FileError, Unit]
  def pullNeeded(git: Git): Either[LockbookError, Boolean]
  def localDirty(git: Git): Boolean
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper, fileHelper: FileHelper) extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  override def cloneRepository(uri: String): Either[LockbookError, Git] = {
    val destination = new File(s"$repoFolder/${uriToFolder(uri)}")

    getCredentials(new URI(uri), askUser = true).flatMap(credentials => {
      try {
        Right(
          Git
            .cloneRepository()
            .setURI(uri)
            .setDirectory(destination)
            .setCredentialsProvider(credentials)
            .call()
        )
      } catch {
        case te: TransportException =>
          // Cleanup
          gitCredentialHelper.deleteStoredCredentials(new URI(uri).getHost)
          fileHelper.recursiveFileDelete(destination)

          // Report error
          Left(TransportExceptionError(te))
        case e: Exception =>
          Left(UnknownException(e))
      }
    })
  }

  override def commit(message: String, git: Git): Either[LockbookError, Unit] = {
    try {
      git.add().addFilepattern(".").call()
      git.commit().setMessage(message).call()
      Right(())
    } catch {
      case e: Exception =>
        Left(UnknownException(e))
    }
  }

  override def push(git: Git): Either[LockbookError, Unit] = {
    getCredentials(git, askUser = true)
      .flatMap(credentials => {
        try {
          git
            .push()
            .setCredentialsProvider(credentials)
            .call()

          Right(())
        } catch {
          case te: TransportException =>
            Left(TransportExceptionError(te))
          case e: Exception =>
            Left(UnknownException(e))
        }
      })
  }

  override def commitAndPush(message: String, git: Git): Either[LockbookError, Unit] = {
    commit(message, git)
      .flatMap(_ => push(git))
  }

  override def sync(git: Git): Either[LockbookError, Unit] = {
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

  private def getCredentials(git: Git, askUser: Boolean): Either[LockbookError, UsernamePasswordCredentialsProvider] =
    getCredentials(getRepoURI(git), askUser)

  private def getCredentials(uri: URI, askUser: Boolean): Either[LockbookError, UsernamePasswordCredentialsProvider] =
    gitCredentialHelper
      .getCredentials(uri.getHost, askUser)
      .map(c => new UsernamePasswordCredentialsProvider(c.username, c.password))

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

  override def pull(git: Git): Either[LockbookError, Unit] = {
    getCredentials(git, askUser = true).flatMap(credentials => {
      try {
        Right(
          git
            .pull()
            .setCredentialsProvider(credentials)
            .call()
        )
      } catch {
        case te: TransportException =>
          Left(TransportExceptionError(te))
        case e: Exception =>
          Left(UnknownException(e))
      }
    })
  }

  override def pullNeeded(git: Git): Either[LockbookError, Boolean] = {
    getCredentials(git, askUser = false)
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
            Left(TransportExceptionError(te))
          case e: Exception =>
            Left(UnknownException(e))
        }
      }
  }

  override def localDirty(git: Git): Boolean = !git.status().call().isClean // TODO Handle failure?
}
