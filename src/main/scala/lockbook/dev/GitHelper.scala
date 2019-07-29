package lockbook.dev

import java.io.File
import java.net.URI

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.{
  JschConfigSessionFactory,
  OpenSshConfig,
  UsernamePasswordCredentialsProvider
}
import org.eclipse.jgit.util.FS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait GitHelper {
  def cloneRepository(uri: String): Try[Git]
  def getRepositories: List[Git]
  def commitAndPush(message: String, git: Git): Try[Unit]
  def getRepoName(git: Git): String
  def pull(git: Git, progressMonitor: ProgressMonitor): Future[Unit]
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper) extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  override def cloneRepository(uri: String): Try[Git] =
    getCredentials(new URI(uri))
      .flatMap(
        credentials =>
          try {
            Success(
              Git
                .cloneRepository()
                .setURI(uri)
                .setDirectory(new File(s"$repoFolder/${uriToFolder(uri)}"))
                .setCredentialsProvider(credentials)
                .call()
            )
          } catch {
            case e: TransportException =>
              gitCredentialHelper.incorrectCredentials(new URI(uri).getHost)
              Failure(new Error("Saved Credentials were invalid, please retry."))
            case e: Exception =>
              Failure(new Error(e))
          }
      )

  override def commitAndPush(message: String, git: Git): Try[Unit] = {
    getCredentials(getRepoURI(git))
      .flatMap(credentials => {
        try {
          git.add().addFilepattern(".").call()
          git.commit().setMessage(message).call()

          git
            .push()
            .setCredentialsProvider(credentials)
            .call()

          Success(Unit)
        } catch {
          case e: Exception =>
            Failure(new Error(e))
        }
      })
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

  override def pull(git: Git, progressMonitor: ProgressMonitor): Future[Unit] = Future {
    getCredentials(git)
      .map(
        credentials => {
          git
            .pull()
            .setCredentialsProvider(credentials)
            .setProgressMonitor(progressMonitor)
            .call()
        }
      )
  }

  private def getCredentials(git: Git): Try[UsernamePasswordCredentialsProvider] =
    getCredentials(getRepoURI(git))

  private def getCredentials(uri: URI): Try[UsernamePasswordCredentialsProvider] = {
    gitCredentialHelper
      .getCredentials(uri.getHost)
      .map(
        gitCredentials =>
          new UsernamePasswordCredentialsProvider(gitCredentials.username, gitCredentials.password)
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
