package lockbook.dev

import java.io.File
import java.net.URI

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.api.{Git, PullCommand}
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.{
  JschConfigSessionFactory,
  OpenSshConfig,
  UsernamePasswordCredentialsProvider
}
import org.eclipse.jgit.util.FS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait GitHelper {
  def cloneRepository(uri: String): Future[Git]
  def getRepositories: Future[List[Git]]
  def commitAndPush(message: String, git: Git): Future[Unit]
  def getRepoName(git: Git): String
  def pullCommand(git: Git, progressMonitor: ProgressMonitor): PullCommand
  def pull(git: Git, pullCommand: PullCommand): Future[Unit]
  def deleteRepo(git: Git): Future[Unit]
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper, fileHelper: FileHelper)
    extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  override def cloneRepository(uri: String): Future[Git] =
    getCredentials(new URI(uri)) transform {
      case Success(s) =>
        s match {
          // Did we get credentials properly?
          case Some(credential) =>
            try {
              Success(
                Git
                  .cloneRepository()
                  .setURI(uri)
                  .setDirectory(new File(s"$repoFolder/${uriToFolder(uri)}"))
                  .setCredentialsProvider(credential)
                  .call()
              )
            } catch {
              case _: TransportException =>
                gitCredentialHelper.deleteStoredCredentials(new URI(uri).getHost)
                Failure(new Error("Saved Credentials were invalid, please retry."))
              case e: Exception =>
                Failure(new Error(e))

            }
          case None => Failure(new Error("Weird error"))

        }
      case Failure(cause) => Failure(cause)
    }

  override def commitAndPush(message: String, git: Git): Future[Unit] =
    getCredentials(git) transform {
      case Success(s) =>
        s match {
          // Did we get credentials properly?
          case Some(credential) =>
            try {
              git.add().addFilepattern(".").call()
              git.commit().setMessage(message).call()

              git
                .push()
                .setCredentialsProvider(credential)
                .call()
              Success(())
            } catch {
              case e: Exception => Failure(new Error(e))
            }
          case None => Failure(new Error("Weird error"))

        }
      case Failure(cause) => Failure(cause)
    }

  override def getRepositories: Future[List[Git]] = Future {
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

  override def pullCommand(git: Git, progressMonitor: ProgressMonitor): PullCommand = {
    git
      .pull()
      .setProgressMonitor(progressMonitor)
  }

  override def deleteRepo(git: Git): Future[Unit] = Future {
    val file = git.getRepository.getWorkTree
    fileHelper.recursiveFileDelete(file)
  }

  private def getCredentials(git: Git): Future[Option[UsernamePasswordCredentialsProvider]] =
    getCredentials(getRepoURI(git))

  private def getCredentials(uri: URI): Future[Option[UsernamePasswordCredentialsProvider]] = {
    gitCredentialHelper
      .getCredentials(uri.getHost)
      .map(
        _.map(
          gitCredential =>
            new UsernamePasswordCredentialsProvider(gitCredential.username, gitCredential.password)
        )
      )
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

  override def pull(git: Git, pullCommand: PullCommand): Future[Unit] = Future {
    println("here")
    getCredentials(git) map {
      case Some(value) =>
        pullCommand
          .setCredentialsProvider(value)
          .call()
      case None =>
        throw new Exception("Weird error")
    }
  }
}

class CustomConfigSessionFactory extends JschConfigSessionFactory {
  override protected def getJSch(hc: OpenSshConfig.Host, fs: FS): JSch = {
    val jsch = super.getJSch(hc, fs)
    jsch.removeAllIdentity()
    jsch.addIdentity("~/.ssh/id_rsa")
    jsch
  }

  override def configure(hc: OpenSshConfig.Host, session: Session): Unit = {}
}
