package devbook

import java.io.File

trait FileExplorer {
  def showFilesIn(file: File): Unit
}
