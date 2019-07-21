package devbook

import java.io.File

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{
  JschConfigSessionFactory,
  OpenSshConfig,
  UsernamePasswordCredentialsProvider
}
import org.eclipse.jgit.util.FS

trait GitHelper {
  def cloneRepository(uri: String): Either[Git, Error]
  def getRepositories: List[Git]
  def commitAndPush(message: String, git: Git)
  def getRepoName(git: Git): String
}

class GitHelperImpl(gitCredentialHelper: GitCredentialHelper) extends GitHelper {

  val repoFolder = s"${App.path}/repos"

  private def getCredentials(uri: String): UsernamePasswordCredentialsProvider = {
    val gitCredentials = gitCredentialHelper.getCredentials(uriToFolder(uri))
    new UsernamePasswordCredentialsProvider(gitCredentials.username, gitCredentials.password)
  }

  override def cloneRepository(uri: String): Either[Git, Error] =
    try {
      Left(
        Git
          .cloneRepository()
          .setURI(uri)
          .setDirectory(new File(s"$repoFolder/${uriToFolder(uri)}"))
          .setCredentialsProvider(getCredentials(uri))
          .call()
      )
    } catch {
      case e: Exception =>
        println(e)
        Right(new Error(e))
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

  override def commitAndPush(message: String, git: Git): Unit = {
    git.add().addFilepattern(".").call() // TODO understand git add -A vs git add .
    git.commit().setMessage(message).call()

    git
      .push()
      .setCredentialsProvider(getCredentials(getRepoName(git)))
      .call()
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
