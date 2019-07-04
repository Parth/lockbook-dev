package devbook

import java.io.File

trait RepositoryList {
  def showRepositoriesIn(file: File): Unit
}
