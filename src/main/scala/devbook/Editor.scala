package devbook

import java.io.File

trait Editor {
  def editFile(file: File): Unit
}
