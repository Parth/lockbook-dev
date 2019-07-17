package devbook

import java.io.File

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{JschConfigSessionFactory, OpenSshConfig, SshTransport, Transport}
import org.eclipse.jgit.util.FS

trait GitHelper {
  def cloneRepository(uri: String): Either[Git, Error]
  def getRepositories: List[Git]
  def commitAndPush(message: String, git: Git)
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
          .setTransportConfigCallback((transport: Transport) => {
            val sshTransport = transport.asInstanceOf[SshTransport]
            sshTransport.setSshSessionFactory(new CustomConfigSessionFactory)
          })
          .call()
      )
    } catch {
      case e: Exception =>
        println(e)
        Right(new Error(e))
    }

  def uriToFolder(uri: String): String = Math.abs(uri.hashCode).toString

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
    git.add().addFilepattern("*").call()
    git.commit().setMessage(message).call()

    git
      .push()
      .setTransportConfigCallback((transport: Transport) => {
        val sshTransport = transport.asInstanceOf[SshTransport]
        sshTransport.setSshSessionFactory(new CustomConfigSessionFactory)
      })
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
