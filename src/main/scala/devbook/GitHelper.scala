package devbook

import java.io.File

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{JschConfigSessionFactory, OpenSshConfig, UsernamePasswordCredentialsProvider}
import org.eclipse.jgit.util.FS

import scala.util.{Failure, Success, Try}

trait GitHelper {
  def cloneRepository(uri: String): Try[Git]
  def getRepositories: List[Git]
  def commitAndPush(message: String, git: Git): Try[Unit]
  def getRepoName(git: Git): String
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper) extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  private def getCredentials(uri: String): Try[UsernamePasswordCredentialsProvider] = {
    gitCredentialHelper
      .getCredentials(uriToFolder(uriToFolder(uri)))
      .map(
        gitCredentials =>
          new UsernamePasswordCredentialsProvider(gitCredentials.username, gitCredentials.password)
      )

  }

  override def cloneRepository(uri: String): Try[Git] =
    getCredentials(uri)
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
            case e: Exception => // TODO need to figure out how to detect login failure to invalidate password java.lang.Error: org.eclipse.jgit.api.errors.TransportException: https://github.com/Parth/private.git: not authorized
              Failure(new Error(e))
          }
      )

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

  override def commitAndPush(message: String, git: Git): Try[Unit] = {
    getCredentials(getRepoName(git))
      .flatMap(credentials => {
        try {
          git.add().addFilepattern(".").call()
          git.commit().setMessage(message).call()

          git
            .push()
            .setCredentialsProvider(credentials)
            .call()

          Success()
        } catch {
          case e: Exception =>
            Failure(new Error(e))
        }
      })

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
