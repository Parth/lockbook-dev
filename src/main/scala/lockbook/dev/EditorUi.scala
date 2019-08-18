package lockbook.dev

import java.io.File

import com.vladsch.flexmark.ast.{Code, Heading, Text}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.CodeArea

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val textArea = new CodeArea()
    val root     = new BorderPane

    loadFile(git, f, root, textArea)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: CodeArea): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setBottom(getBottom(git, f, text))
          root.setCenter(text)
          text.replaceText(fileText)
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def doMarkdown(text: CodeArea): Unit = {
    val parser: Parser = Parser.builder().build()

    text
      .textProperty()
      .addListener((_, _, newText) => {
        val parsed = parser.parse(newText)
        nodeVisitor(text).visit(parsed)
      })
  }

  private def getBottom(git: Git, file: File, textArea: CodeArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {
      Future {
        editorHelper
          .saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) match {
          case Right(_) =>
            Platform.runLater(() => {
              AlertUi.showGood("Push Successful", "Changes saved successfully.")
            })
          case Left(exception) =>
            Platform.runLater(() => {
              AlertUi.showBad("Push Failed", exception.uiMessage)
            })
        }
      }
    })

    new HBox(commitMessage, save)
  }

  private val nodeVisitor: CodeArea => NodeVisitor = (styledText: CodeArea) =>
    new NodeVisitor(new VisitHandler[Heading](classOf[Heading], (node: Heading) => {
      println(s"heading: ${node.getChars}")
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getText.getEndOffset, s"h${node.getLevel}")
    }), new VisitHandler[Text](classOf[Text], (node: Text) => {
      println(s"Text: ${node.getChars}")
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "text")
    }), new VisitHandler[Code](classOf[Code], (node: Code) => {
      println(s"Code: ${node.getChars}")
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code")
    }))
}
